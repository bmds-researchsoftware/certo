(ns certo.models.default
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]   
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]
   [certo.utilities :as cu])
  (:import [java.util.UUID]))


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


;; Since schema and table "whitelisted" in the controller we don't
;; need to check schema and table in the functions here.


;; All fields with is_settable=false are removed from the (form)
;; params and do not appear on the new form, and ll fields with
;; is_settable=false are removed from the (form) params but they do
;; appear on the edit form, and the id, even if is_settable=false is
;; used in where clause for update and delete statements.


(defn st [schema table]
  (str schema "." table))


(defn stf [schema table field]
  (str schema "." table "." field))


(defn- sql-identifier [field]
  "If the field is a Postgresql function, then it must be referenced
  in the where clause of update and delete statements as
  schema_name.table_name.field_name, otherwise it must be referenced
  as field_name."
  (if (:is_function (val field))
    (key field)
    (:field_name (val field))))


(defn id-field [fields schema table]
  "Returns the map entry in fields that is the primary key for schema.table"
  (let [idfs
        (into
         {}
         (filter
          (fn [[k v]] (and (:is_id v) (= (:schema_name v) schema) (= (:table_name v) table)))
          fields))]
    (case (count (take 2 idfs))
      0 (throw (Exception. "id-field::none found, but expected exactly one."))
      1 (first idfs)
      (throw (Exception. "id-field::multiple found, but expected exactly one.")))))


(defn ui-to-db-one [fields field value]
  "The values are always strings."
  (when (not (get fields field))
    (throw (Exception. (format "ui-to-db-one:: unknown field: %s" field))))
  (if (not (= value ""))
    (let [type (:type (get fields field))]
      ;; TO DO: See if this is faster if type is a keyword or if use a multimethod?
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
        ;; "serial8" (Long/parseLong value)
        "serial8" (Long/parseLong (str/replace value #"[.][0]+$|[.]+$" ""))
        "text" value
        "timestamptz"
        (let [value (str/replace value #"T" " ")]
          (try
            (java.sql.Timestamp/valueOf value)
            ;; java.lang.IllegalArgumentException: Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
            (catch java.lang.IllegalArgumentException e1
              (try
                (java.sql.Timestamp/valueOf (str value ":00"))
                (catch java.lang.IllegalArgumentException e2
                  (throw e1))))))
        "uuid" (java.util.UUID/fromString value)
        (throw (Exception. (format "Unknown type %s for field %s with value %s" type field value)))))
    nil))


(defn ui-to-db [fields params]
  "Called by insert! and update!"
  (let [fs (clojure.set/intersection
           ;; fs is the intersection of the form parameter fields and
           ;; the fields in this schema.table that are settable; all
           ;; fields that are not settable, e.g. functions, are excluded.
           (set (map name (keys params)))
           (set (keys (filter (fn [[k v]] (:is_settable v)) fields))))]
      (into
       {}
       (map
        (fn [f]
          ;; f is of the form "schema.table.field", drop schema and
          ;; table so just have "field" which is the form required for
          ;; the generated SQL statements
          [(str/join "." (drop 2 (str/split f #"[.]")))
           (ui-to-db-one fields f (get params f))])
        fs))))


(defn tables [db & [schema table]]
  (jdbc/query
   db
   (if (and schema table)
     ["select * from sys.tables where schema_name=? and table_name=?" schema table]
     ["select * from sys.tables"])
   {:row-fn (fn [row] (vector (:tables_id row) row))
    :result-set-fn (fn [rs] (into {} rs))}))


;; TO DO: Confirm db constraint will prevent schema_table_field and
;; options_schema_table from being nil, and then remove the nil? check
;; below
(defn select-options [db schema_table_field select_option_schema_table]
  {schema_table_field
   {:options
    (if (or (nil? schema_table_field) (nil? select_option_schema_table))
      []
      (jdbc/query db [(format "select * from %s" select_option_schema_table)] {:row-fn (juxt :label :value)}))}})


;; TO DO: Confirm db constraint will prevent schema_table_field,
;; foreign_key_schema_table, and foreign_key_field from being nil, and
;; then remove the nil? check below
(defn select-results [db schema_table_field value]
  {schema_table_field
   {:options
    (if (or (nil? schema_table_field) (nil? value))
      []
      (jdbc/query
       db
       ;; TO DO: The Postgresql function should be passed the "parent_id"
       ;; TO DO: value is read from an html form so watch sql injection
       [(format "select * from %s(null)" value)]
       {:row-fn (fn [{:keys [:value] :as all}] {:value value :label-fields all})}))}})


(defn common-fields [db & [schema table]]
  (into
   {}
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
        :size "22"
        :is_settable true
        :disabled true
        :readonly true
        :required false}

       (str schema "." table ".created_at")
       {:schema_name schema
        :table_name table
        :field_name "created_at"
        :type "timestamptz"
        :label "Created At"
        :control "timestamp"
        :location (- Long/MAX_VALUE 2)
        :in_table_view false
        :size "22"
        :is_settable false
        :disabled true
        :readonly true
        :required false}

       (str schema "." table ".updated_by")
       {:schema_name schema
        :table_name table
        :field_name "updated_by"
        :type "text"
        :label "Updated By"
        :control "text"
        :location (- Long/MAX_VALUE 1)
        :in_table_view false
        :size "22"
        :is_settable true
        :disabled true
        :readonly true
        :required false}

       (str schema "." table ".updated_at")
       {:schema_name schema
        :table_name table
        :field_name "updated_at"
        :type "timestamptz"
        :label "Updated At"
        :control "timestamp"
        :location Long/MAX_VALUE
        :in_table_view false
        :size "22"
        :is_settable false
        :disabled true
        :readonly true
        :required false}})
    (jdbc/query
     db
     (if (and schema table)
       ["select schema_name, table_name from sys.tables where schema_name=? and table_name=?" schema table]
       ["select schema_name, table_name from sys.tables"])
     {:row-fn (juxt :schema_name :table_name)}))))


(defn sort-by-location [fields]
  (into
   (sorted-map-by
    (fn [key1 key2]
      (-
       (compare [(get-in fields [key2 :location]) key2]
                [(get-in fields [key1 :location]) key1]))))
   fields))


(defn table-fields [db & [schema table]]
  (merge-with

   merge

   (common-fields db schema table)

   (jdbc/query
    db
    (if (and schema table)
      ["select * from sys.fields where schema_name=? and table_name=?" schema table]
      ["select * from sys.fields"])
    {:row-fn (fn [row] (vector (:fields_id row) row))
     :result-set-fn (fn [rs] (into {} rs))})

   (jdbc/query
    db
    ;; Get a list of the fields that have a select control
    (if (and schema table)
      ["select * from sys.fields where control='select-option' and schema_name=? and table_name=?" schema table]
      ["select * from sys.fields where control='select-option'"])
    {:row-fn
     (fn [row] (select-options db (:fields_id row) (:select_option_schema_table row)))
     :result-set-fn (fn [rs] (into {} rs))})

   ;; TO DO: You should definitely not be carrying around this data.
   ;; Most, importantly this needs to be updated before using in a
   ;; form, since it may be been changed. Add it on in the controller
   ;; when editing a record or making a new one.
   (jdbc/query
    db
    ;; TO DO: Can you use a reducibe-query here?
    ;; Get a list of the fields that have a select-result control
    (if (and schema table)
      ["select fs.fields_id, qs.value, qs.label from sys.fields as fs inner join sys.options_select_result_function_names as qs on fs.select_result_function_name=qs.value where fs.control='select-result' and schema_name=? and table_name=?" schema table]      
      ["select fs.fields_id, qs.value, qs.label from sys.fields as fs inner join sys.options_select_result_function_names as qs on fs.select_result_function_name=qs.value where fs.control='select-result'"])
    {:row-fn (fn [row] (select-results db (:fields_id row) (:value row)))
     :result-set-fn (fn [rs] (into {} rs))})))


;; TO DO:
;; Can you use reducible-query, maybe a function in models returns a reducible query
;; and a function in controller or view transforms the it by calling db-to-ui for select-result


(defn fields [db & [schema table]]
  "Update each view field with information from the corresponding
  table field and then merge view fields with table fields."
  (let [vfs
        (jdbc/query ;; TO DO: Use reducible-query
         db
         (if (and schema table)
           ["select * from sys.view_fields where schema_name=? and table_name=?" schema table]
           ["select * from sys.view_fields"]))

        ;; TO DO: Use a join between sys.tables, sys.view_fields, and
        ;; sys.fields to retrieve only the required rows from
        ;; sys.fields, i.e. "select st.is_view, svf.view_fields_id,
        ;; sf.* from sys.tables as st inner join sys.view_fields as
        ;; svf on (st.schema_name=svf.schema_name and
        ;; st.table_name=svf.table_name) inner join sys.fields as sf
        ;; on svf.fields_id=sf.fields_id where st.is_view='true' and
        ;; st.schema_name=? and st.table_name=?"

        ;; if (not (empty? vfs)) then schema.table is a view and we
        ;; need all table-fields until we implement the optimization
        ;; described above
        tfs (if (empty? vfs)
              (table-fields db schema table)
              (table-fields db))
        vfs
        (into
         {}
         (map
          (fn [vf]
            (vector
             (:view_fields_id vf)
             (let [tf (get tfs (:fields_id vf))]
               ;; if :is_id is true in the table field, then in the
               ;; view field, set :is_uk to true and
               ;; :search_fields_id, to be the value :fields_id from
               ;; the table field
               (as-> vf vf
                 (merge tf vf)
                 (if (:is_id tf)
                   (assoc vf :is_uk true :search_fields_id (:fields_id tf))
                   vf)
                 (dissoc vf :view_fields_id :fields_id :is_id)
                 (assoc vf :fields_id (:view_fields_id vf))))))
          vfs))]
    (merge tfs vfs)))


(defn fields-by-schema-table [fields schema table]
  (into {} (filter (fn [[k v]] (and (= (:schema_name v) schema) (= (:table_name v) table))) fields)))


(defn fields-by-schema-table-and-in-table-view [fields schema table]
  (into {} (filter (fn [[k v]] (and (= (:schema_name v) schema) (= (:table_name v) table) (:in_table_view v))) fields)))

(defn fields-in-table-view [fields]
  (into {} (filter (fn [[k v]] (:in_table_view v)) fields)))



(defn field-by-event [fields event]
  )


(defn where-clause
  "Returns [where-string param1 param2 param3 ...]"
  ([fields schema table id where?]
   (let [idf (id-field fields schema table)]
     [(str (if where? "where " "") (sql-identifier idf) " = ?") (ui-to-db-one fields (key idf) id)]))
  ([fields params where?]
   "Since schema and table are not arguments to this function, it can
   used to create the where clause for a view."
   (if (empty? params)
     [""]
     (let [operator (or (#{"and" "or"} (get params "operator"))
                        (throw (Exception. "Operator is invalid or not specified")))
           comparator (or (#{"exact" "approximate"} (get params "comparator"))
                          (throw (Exception. "Comparator is invalid or not specified")))]
       (loop [params (dissoc params "operator" "comparator")
              where-strings []
              where-params []]
         (if (not params)
           (if (empty? where-params)
             [""]
             (into [(str (if where? "where " "") (str/join (str " " operator " ") where-strings))] where-params))
           (let [[stf value] (first params)
                 value (ui-to-db-one fields stf value)]
             (if (nil? value)
               (recur
                (next params)
                where-strings
                where-params)
               (recur
                (next params)
                (if (and (= comparator "approximate") (= (:type (get fields stf)) "text"))
                  (conj where-strings (str stf " ilike ?"))
                  (conj where-strings (str stf " = ?")))
                (if (and (= comparator "approximate") (= (:type (get fields stf)) "text"))
                  (conj where-params (str "%" value "%"))
                  (conj where-params value)))))))))))


(defn columns-clause [fields schema table]
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


;; TO DO: Considering combining (select) and (select-by-id).

;; TO DO: Add the ability to specify an order by clause via query-params

;; TO DO: Store a default order by clause for each table in sys.tables.


(defn select [db fields tables schema table params]
  (let [[where-string & where-parameters] (where-clause fields params true)
        rs
        (jdbc/query
         db
         (into
          (vector
           (str
            (if (get-in tables [(st schema table) :is_view])
              (str "select * from " (get-in tables [(st schema table) :tables_id]))
              (columns-clause fields schema table))
            " "
            where-string))
          where-parameters))]
    (if (empty? rs)
      (throw (Exception. "None found"))
      rs)))


(defn select-by-id [db fields schema table id]
  (let [[where-string & where-parameters] (where-clause fields schema table id true)
        rs
        (jdbc/query
         db
         (into
          (vector
           (str
            (columns-clause fields schema table)
            " "
            where-string))
          where-parameters))]
    (case (count (take 2 rs))
      0 (throw (Exception. "None found, but expected exactly one."))
      1 (first rs)
      (throw (Exception. "Multiple found, but expected exactly one.")))))


(defn select-all [db fields schema table]
  (let [rs
        (jdbc/query
         db
         [(columns-clause fields schema table)])]
    (if (empty? rs)
      (throw (Exception. "None found"))
      rs)))


(defn insert! [db fields schema table params]
  (let [rs
        (jdbc/insert!
         db
         (st schema table)
         (ui-to-db fields params))]
    (case (count (take 2 rs))
      0 (throw (Exception. "Error: Not inserted."))
      1 true
      (throw (Exception. "Warning: Unexpected result on insert.")))))


(defn update! [db fields schema table params id]
  (let [params (ui-to-db fields params)]
    (let [[cnt]
          (jdbc/update!
           db
           (st schema table)
           params
           (where-clause fields schema table id false))]
      (case cnt
        0 (throw (Exception. "Error: Not updated."))
        1 true
        (throw (Exception. "Warning: Unexpected result on update, %s rows affected." cnt))))))


(defn delete! [db fields schema table id]
  (let [[cnt]
        (jdbc/delete!
         db
         (st schema table)
         (where-clause fields schema table id false))]
    (case cnt
      0 (throw (Exception. "Error: Not deleted."))
      1 true
      (throw (Exception. (format "Warning: Unexpected result on delete, %s rows affected." cnt))))))


(defn select-count-star [db st]
  (jdbc/query db [(format "select count(*) as cnt from %s" st)] {:row-fn :cnt :result-set-fn first}))


(defn dashboard [db]
  "Returns:
  (\"Schema_1\" ([table_1_1 is_view_1_1 count_1_1] [table_1_2 is_view_1_2 count_1_2])
   \"Schema_2\" ([table_2_1 is_view_2_1 count_2_1] [table_2_2 is_view_2_2 count_2_2] [table_2_3 is_view_2_3 count_2_3]))"
  (let [schemas
        (jdbc/query db ["select schema_name from sys.tables group by schema_name order by schema_name"] {:row-fn :schema_name})]
    (map
     (fn [schema]
       [schema 
        (jdbc/query
         db
         ["select table_name, is_view from sys.tables where schema_name=? group by table_name, is_view order by table_name"
          schema]
         {:row-fn #(let [table (:table_name %)
                         st (st schema table)]
                     {:table table
                      :is_view (:is_view %)
                      :count (select-count-star db st)})})])
     schemas)))

