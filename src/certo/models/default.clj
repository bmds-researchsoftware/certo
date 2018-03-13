(ns certo.models.default
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]   
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]
   [certo.utilities :as cu])
  (:import [java.util.UUID]))


;; Since schema and table "whitelisted" in the controller we don't
;; need to check schema and table in the functions here.


;; Something like this would work and is easy to implement, but it
;; makes a call to getParameterMetaData (~0.5ms) for each column in
;; the PreparedStatement so it's too slow.
;; (extend-protocol jdbc/ISQLParameter
;;   java.lang.Object
;;   (set-parameter [v ^java.sql.PreparedStatement stmt ^long i]
;;     (let [param-meta (time (.getParameterMetaData stmt))
;;           param-type-name (time (.getParameterTypeName param-meta i))]
;;       (println (str "param-type-name = " param-type-name ", i = " i ", class = " (class v) ", value = " v))
;;       (cond (= param-type-name "date")
;;             (.setObject stmt i (java.sql.Date/valueOf v))
;;             (= param-type-name "int8")
;;               ;; (.setObject stmt i (Integer/parseInt v))
;;               (.setObject stmt i (Long/parseLong v))
;;             :else (.setObject stmt i v)))))


(defn st [schema table]
  (str schema "." table))


(defn stf [schema table field]
  (str schema "." table "." field))


(defn pk-field-name [fields schema table]
  (let [pks
        (map
         val
         (into
          {}
          (filter
           (fn [[k v]] (and (:is_pk v) (= (:schema_name v) schema) (= (:table_name v) table)))
           fields)))]
    (case (count (take 2 pks))
      0 (throw (Exception. "None found, but expected exactly one."))
      1 (:field_name (first pks))
      (throw (Exception. "Multiple found, but expected exactly one.")))))


(defn stpk [fields schema table]
  (str schema "." table "." (pk-field-name fields schema table)))


(defn ui-to-db-one [fields field value]
  (if (not (= value ""))
    (let [type (:type (get fields field))]
      (case type
        "boolean" (cond
                    (= value "true") true
                    (= value "false") false
                    :else
                    (throw (Exception. (format "Invalid boolean value %s for field %s" value field))))
        "date" (java.sql.Date/valueOf value)
        "float8" (Double/parseDouble value)
        ;; "int8" (Long/parseLong value)
        "int8" (Long/parseLong (str/replace value #"[.][0]+$|[.]+$" ""))
        "serial8" (Long/parseLong value)
        "text" value
        "timestamptz" (java.sql.Timestamp/valueOf value)
        "uuid" (java.util.UUID/fromString value)
        (throw (Exception. (format "Unknown type %s for field %s with value %s" type field value)))))
    nil))


(defn ui-to-db [fields params]
  (let [fs (clojure.set/intersection (set (map name (keys params))) (set (keys fields)))]
    (into
     {}
     (map
      (fn [f]
        [;; f is of the form "schema.table.field", drop schema and table so just have "field"
         (str/join "." (drop 2 (str/split f #"[.]")))
         (ui-to-db-one fields f (get params f))])
      fs))))


;; TO DO: Remove if nil?
(defn select-options [db {:keys [:schema_table_field :options_schema_table]}]
  {schema_table_field
   {:options
    (if (or  (nil? schema_table_field) (nil? options_schema_table))
      []
      (jdbc/query db [(format "select * from %s" options_schema_table)] {:row-fn (juxt :label :value)}))}})


(defn common-fields [db]
  (reduce
   into
   (map
    (fn [[schema table]]
      {(str schema "." table ".created_by")
       {:schema_name schema
        :table_name table
        :field_name "created_by"
        :type "text"
        :label "Created By"
        :control "text"
        :location (- Long/MAX_VALUE 3)
        :in_table_view false
        :disabled true}

       (str schema "." table ".created_at")
       {:schema_name schema
        :table_name table
        :field_name "created_at"
        :type "timestamptz"
        :label "Created At"
        :control "timestamp"
        :location (- Long/MAX_VALUE 2)
        :in_table_view false
        :disabled true}

       (str schema "." table ".updated_by")
       {:schema_name schema
        :table_name table
        :field_name "updated_by"
        :type "text"
        :label "Updated By"
        :control "text"
        :location (- Long/MAX_VALUE 1)
        :in_table_view false
        :disabled true}       

       (str schema "." table ".updated_at")
       {:schema_name schema
        :table_name table
        :field_name "updated_at"
        :type "timestamptz"
        :label "Updated At"
        :control "timestamp"
        :location Long/MAX_VALUE
        :in_table_view false
        :disabled true}})
    (jdbc/query
     db
     ["select schema_name, table_name from sys.fields group by schema_name, table_name"]
     {:row-fn (juxt :schema_name :table_name)}))))


(defn sort-by-location [fields]
  (into
   (sorted-map-by
    (fn [key1 key2]
      (-
       (compare [(get-in fields [key2 :location]) key2]
                [(get-in fields [key1 :location]) key1]))))
   fields))


(defn fields [db]
  (merge-with
   merge
   (into
    (common-fields db)
    (into
     {}
     (map
      ;; TO DO: Maybe do with a row-fn
      (fn [{schema-name :schema_name table-name :table_name field-name :field_name :as row}]
        [(str schema-name "." table-name "." field-name) row])
      (jdbc/query db ["select * from sys.fields"]))))
   (reduce
    into
    (map
     #(select-options db %)
     ;; Get a list of the fields that have a select control
     (jdbc/query
      db 
      ["select * from sys.fields where control='select'"]
      {:row-fn
       #(hash-map
         :schema_table_field (str (:schema_name %) "." (:table_name %) "." (:field_name %))
         :options_schema_table (:options_schema_table %))})))))


(defn fields-by-schema-table [fields schema table]
  (into {} (filter (fn [[k v]] (and (= (:schema_name v) schema) (= (:table_name v) table))) fields)))


(defn fields-by-schema-table-and-in-table-view [fields schema table]
  (into {} (filter (fn [[k v]] (and (= (:schema_name v) schema) (= (:table_name v) table) (:in_table_view v))) fields)))


(defn field-by-event [fields event]
  )


(defn select-all-statement [fields schema table]
  "This function does a lookup on schema and table in the fields
  hashmap.  Since no database query is run, and it throws an exception
  when no fields are found for the given schema and table, it avoids
  the risk of an SQL injection attack."
  (let [flds (fields-by-schema-table fields schema table)]
    (if (not (empty? flds))
      (format
       "select %s from %s.%s"
       (str/join
        ", "
        (map
         #(str (str/join "." %) " as " "\"" (str/join "." %) "\"")
         (map
          #((juxt :schema_name :table_name :field_name) %)
          (map val flds))))
       schema table)
      (throw (Exception. (format "No fields found for schema: %s and table: %s" schema table))))))


(defn pk-clause [fields schema table]
  "Returns primary_key_field_name=?"
  (format "%s=?" (pk-field-name fields schema table)))


(defn select-one-statement [fields schema table]
  (str (select-all-statement fields schema table) " where " (pk-clause fields schema table)))


(defn select-all [db {:keys [fields]} schema table]
  (let [rs
        (jdbc/query
         db
         [(select-all-statement fields schema table)])]
    (if (empty? rs)
      (throw (Exception. "None found"))
      rs)))


(defn select-one [db {:keys [fields]} schema table pk]
  (let [rs
        (jdbc/query
         db
         [(select-one-statement fields schema table)
          (ui-to-db-one fields (stpk fields schema table) pk)])]
    (case (count (take 2 rs))
      0 (throw (Exception. "None found, but expected exactly one."))
      1 (first rs)
      (throw (Exception. "Multiple found, but expected exactly one.")))))


(defn insert! [db {:keys [fields]} schema table params]
  (let [rs
        (jdbc/insert!
         db
         (st schema table)
         (ui-to-db fields params))]
    (case (count (take 2 rs))
      0 (throw (Exception. "Error: Not inserted."))
      1 true
      (throw (Exception. "Warning: Unexpected result on insert.")))))


(defn update! [db {:keys [fields]} schema table params]
  (let [params (ui-to-db fields params)
        pkfn (pk-field-name fields schema table)]
    (let [[cnt]
          (jdbc/update!
           db
           (st schema table)
           params
           [(pk-clause fields schema table) (get params pkfn)])]
      (case cnt
        0 (throw (Exception. "Error: Not updated."))
        1 true
        (throw (Exception. "Warning: Unexpected result on update."))))))


(defn delete! [db {:keys [fields]} schema table pk]
  (let [[cnt]
        (jdbc/delete!
         db
         (st schema table)
         [(pk-clause fields schema table)
          (ui-to-db-one fields (stpk fields schema table) pk)])]
    (case cnt
      0 (throw (Exception. "Error: Not deleted."))
      1 true
      (throw (Exception. "Warning: Unexpected result on delete.")))))


(defn select-count-star [db st]
  (jdbc/query db [(format "select count(*) as cnt from %s" st)] {:row-fn :cnt :result-set-fn first}))


;; Returns:
;; ("Schema_1" ([table_1_1 count_1_1] [table_1_2 count_1_2])
;; "Schema_2" ([table_2_1 count_2_1] [table_2_2 count_2_2] [table_2_3 count_2_3]))
(defn dashboard [db md]
  (let [schemas
        (jdbc/query db ["select schema_name from sys.fields group by schema_name order by schema_name"] {:row-fn :schema_name})]
    (map
     (fn [schema]
       [schema 
        (jdbc/query
         db ["select table_name from sys.fields where schema_name=? group by table_name order by table_name" schema]
         {:row-fn #(let [table (:table_name %)
                         st (st schema table)]
                     [table (select-count-star db st)])})])
     schemas)))

