(ns certo.models.default
  (:require
   [jsonista.core :as json]
   [clojure.string :as str]
   [clojure.set :as set]
   [clojure.edn :as edn]
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]
   [java-time :as jt]
   [taoensso.timbre :as log]
   [certo.sql]
   [certo.sql-events]
   [certo.utilities :as cu])
  (:import (java.util UUID)))


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
      0 (throw (Exception. (format "id-field::none found, but expected exactly one for schema: %s and table: %s." schema table)))
      1 (first idfs)
      (throw (Exception. (format "id-field::multiple found, but expected exactly one for schema: %s and table: %s.." schema table))))))


(defn ui-to-db-one [fields field value]
  "The values are always strings."
  (when (not (get fields field))
    (throw (Exception. (format "ui-to-db-one:: unknown field: %s with value: %s" field value))))
  (if (not (= value ""))
    (let [type (:type (get fields field))]
      ;; TO DO: See if this is faster if type is a keyword or if use a multimethod?
      (case type
        "boolean" (cond
                    (= value "true") true
                    (= value "false") false
                    :else
                    (throw (Exception. (format "Invalid boolean value %s for field %s" value field))))
        "date" (jt/local-date "yyyy-MM-dd" value)
        "float8" (Double/parseDouble value)
        ;; "int8" (Long/parseLong value)
        "int8" (Long/parseLong (str/replace value #"[.][0]+$|[.]+$" ""))
        "jsonb"
        (doto (org.postgresql.util.PGobject.)
          (.setType "json")
          (.setValue
           (try
             ;; check if already valid json
             (json/read-value value)
             ;; if already valid json just return it
             value
             ;; if not already valid json try to generate valid json
             (catch com.fasterxml.jackson.core.JsonProcessingException e
               (json/write-value-as-string value (json/object-mapper {:date-format "yyyy-MM-dd"}))))))
        ;; "serial8" (Long/parseLong value)
        "serial8" (Long/parseLong (str/replace value #"[.][0]+$|[.]+$" ""))
        "text" value
        "time" (jt/local-time value)

        "timestamptz" (jt/local-date-time value)

        ;; TO DO: Can you change this to use?
        ;; (jt/local-date-time "yyyy-MM-dd HH:mm:ss" "1963-12-30 01:02:03")?
        ;; If do this, confirm time zone is stored the same way it is now?

        ;; (let [value (str/replace value #"T" " ")]
        ;;   (try
        ;;     (java.sql.Timestamp/valueOf value)
        ;;     ;; java.lang.IllegalArgumentException: Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]
        ;;     (catch java.lang.IllegalArgumentException e1
        ;;       (try
        ;;         (java.sql.Timestamp/valueOf (str value ":00"))
        ;;         (catch java.lang.IllegalArgumentException e2
        ;;           (throw e1))))))

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
     ["select * from sys.tables where schema_name = ? and table_name = ?" schema table]
     ["select * from sys.tables"])
   {:row-fn
    (if (and schema table)
      (fn [row] row)
      (fn [row] (vector (:tables_id row) row)))
    :result-set-fn
    (if (and schema table) (fn [rs] (first rs)) (fn [rs] (into {} rs)))}))


(defn event-class-dimensions [db dimensions-type & [event_classes_id]]
  (assert (or (= dimensions-type :argument) (= dimensions-type :result)) "Event class dimensions type not :argument or :result")
  (jdbc/query
   db
   (concat
    [(str
      (format "select * from app.event_class_%s_dimensions" (name dimensions-type))
      (if event_classes_id
        " where event_classes_id=?"
        ""))]
    (if event_classes_id
      [event_classes_id]
      []))
   {:row-fn
    (fn [row]
      (vector
       (:event_classes_id row)
       (mapv key (filter (fn [[k v]] v) (dissoc row :event_classes_id :created_by :created_at :updated_by :updated_at)))))
    :result-set-fn
    (fn [rs]
      (if event_classes_id
        (second (first rs))
        (into {} rs)))}))


(defn event-classes [db & [event_classes_id]]
  (jdbc/query
   db
   (if event_classes_id
     ["select * from sys.event_classes where event_classes_id = ?" event_classes_id]
     ["select * from sys.event_classes"])
   {:row-fn
    (if event_classes_id
      (fn [row] row)
      (fn [row] (vector (:event_classes_id row) row)))
    :result-set-fn
    (if event_classes_id
      (fn [rs] (first rs))
      (fn [rs] (into {} rs)))}))


(defn select-options [db row]
  (assoc
   row
   :options
   (jdbc/query
    db
    [(format "select * from %s" (:select_option_table row))]
    {:row-fn (juxt :label :value)})))


(defn select-results [db row]
  (assoc
   row
   :options
   (hash-map
    :transducer
    (fn [r]
      (into
       {}
       (map
        (fn [[k v]]
          (vector
           (if (= k :value)
             k
             (str (:select_result_view row) "." (clojure.core/name k)))
           v))
        r)))
    :reducible-query
    (jdbc/reducible-query
     db
     ;; TO DO: The Postgresql function should be passed the
     ;; "parent_id" TO DO: select_result_view is read from an html
     ;; form so watch sql injection
     [(format "select * from %s" (:select_result_view row))]))))


(defn common-fields [db schema table]
  {(str schema "." table ".created_by")
   {:fields_id (stf schema table "created_by")
    :schema_name schema
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
   {:fields_id (stf schema table "created_at")
    :schema_name schema
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
   {:fields_id (stf schema table "updated_by")
    :schema_name schema
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
   {:fields_id (stf schema table "updated_at")
    :schema_name schema
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


(defn common-event-fields [db schema table is-negatable require-time]
  (cond->
      {(str schema "." table ".event_queue_id")
       {:fields_id (stf schema table "event_queue_id")
        :schema_name schema
        :table_name table
        :field_name "event_queue_id"
        :type "int8"
        :label "Event Queue ID"
        :control "integer"
        :integer_step 1
        :integer_min 0
        :integer_max nil
        :location Long/MIN_VALUE
        :size 6
        :is_settable true
        :disabled false
        :readonly true
        :required true}

       (str schema "." table ".event_by")
       (select-results
        db
        {:fields_id (stf schema table "event_by")
         :schema_name schema
         :table_name table
         :field_name "event_by"
         :type "text"
         :label "Event By"
         :control "select-result"
         :location (+ Long/MIN_VALUE 1)
         :size "22"
         :is_settable true
         :disabled false
         :readonly false
         :required true
         :select_multiple false
         :select_size 5
         :select_result_view "sys.rv_users"})

       (str schema "." table ".event_date")
       {:fields_id (stf schema table "event_date")
        :schema_name schema
        :table_name table
        :field_name "event_date"
        :type "date"
        :label "Event Date"
        :control "date"
        :location (+ Long/MIN_VALUE 2)
        :size "22"
        :is_settable true
        :disabled false
        :readonly false
        :required true}

       (str schema "." table ".event_notes")
       {:fields_id (stf schema table "event_notes")
        :schema_name schema
        :table_name table
        :field_name "event_notes"
        :type "text"
        :label "Event Notes"
        :control "textarea"
        :location Long/MAX_VALUE
        :is_settable true
        :disabled false
        :readonly false
        :required false
        :textarea_cols 80
        :textarea_rows 10
        :size "22"}}

      require-time
      (assoc
       (str schema "." table ".event_time")
       {:fields_id (stf schema table "event_time")
        :schema_name schema
        :table_name table
        :field_name "event_time"
        :type "time"
        :label "Event Time"
        :control "time"
        :location (+ Long/MIN_VALUE 3)
        :size "22"
        :is_settable true
        :disabled false
        :readonly false
        :required true})

      is-negatable
      (assoc
       (str schema "." table ".is_event_done")
       {:fields_id (stf schema table "is_event_done")
        :schema_name schema
        :table_name table
        :field_name "is_event_done"
        :type "boolean"
        :label "Is Event Done?"
        :control "select-boolean"
        :boolean_true "Yes"
        :boolean_false "No"
        :location (+ Long/MIN_VALUE 4)
        :size "22"
        :is_settable true
        :disabled false
        :readonly false
        :required true}

       (str schema "." table ".event_not_done_reason")
       (select-options
        db
        {:fields_id (stf schema table "event_not_done_reason")
         :schema_name schema
         :table_name table
         :field_name "event_not_done_reason"
         :type "text"
         :label "Event Not Done Reason"
         :control "select-option"
         :select_option_table "sys.ot_event_not_done_reasons"
         :select_multiple false
         :select_size 1
         :location (+ Long/MIN_VALUE 5)
         :size "22"
         :is_settable true
         :disabled false
         :readonly false
         :required false}))))


(defn sort-by-location [fields]
  (into
   (sorted-map-by
    (fn [key1 key2]
      (-
       (compare [(get-in fields [key2 :location]) key2]
                [(get-in fields [key1 :location]) key1]))))
   fields))


(defn prepare-control [row db]
  (cond
    (= (:control row) "select-option")
    (select-options db row)
    (= (:control row) "select-result")
    (select-results db row)
    :else row))


(defn merge-into-sys-fields [db row in-select-result]
  "Make a new sys-fields row by merging values in a sys.field_sets or
  sys.event_class_fields row into the corresponding sys.fields row."
  (vector

   (:vf_fields_id row)

   (cond->
       (reduce
        (fn [row k]
          (-> row
              (assoc (keyword k) (get row (keyword (str "vf_" k))))
              (dissoc (keyword (str "vf_" k)))))
        row
        ["fields_id" "tables_id" "schema_name" "table_name" "field_name"
         "label" "location"
         "created_by" "created_at" "updated_by" "updated_at"])

     ;; make :in_table_view=true so that fields which are in a
     ;; select-result will be displayed (primarily during
     ;; development) in a table view of the underlying view.
       in-select-result (assoc :in_table_view true)

       ;; if :is_id is true in sys.fields, then in the view, set :is_id
       ;; to false, :is_uk to true and, :search_fields_id to be the
       ;; value :fields_id from the sys.fields.
       (:is_id row) (assoc :is_id false :is_uk true :search_fields_id (:fields_id row))

       ;; make :is_settable=true so that fields which have
       ;; :is_settable=false in sys.fields, will be displayed in new
       ;; forms as a select-result control, e.g. when the field is a
       ;; foreign key in the new table.
       :always (assoc :is_settable true)
       :always (assoc :disabled false)

       ;; dissoc since sys_field_id is not a field in sys.fields
       :always (dissoc :vf_sys_fields_id)

       :always (prepare-control db))))


(defn fields [db schema table table-map]
  (let [{is_table :is_table
         is_option_table :is_option_table
         is_view :is_view
         is_result_view :is_result_view
         is_negatable :is_negatable
         is_time_required :is_time_required
         :or {is_table false is_option_table false is_view false is_result_view false is_negatable false is_time_required false}}
        table-map
        is_event (= schema "event")]

    (merge

     (if (or is_table is_option_table is_event)
       (common-fields db schema table)
       {})

     (if is_event
       (common-event-fields db schema table is_negatable is_time_required)
       {})

     (cond
       (or is_view is_result_view)
       ;; get all controls in the view schema.table
       (certo.sql/select-sys-fields-sets-by-schema-table
        db
        {:schema schema :table table}
        {}
        {:row-fn #(merge-into-sys-fields db % is_result_view)
         :result-set-fn (fn [rs] (into {} rs))})

       is_event
       ;; get all controls in the event class with event-class-id table
       (certo.sql/select-sys-event-class-fields-by-event-classes-id
        db
        {:event_classes_id table}
        {}
        {:row-fn #(merge-into-sys-fields db % is_result_view)
         :result-set-fn (fn [rs] (into {} rs))})

       :else
       (jdbc/query
        db
        ;; get all controls in the table schema.table
        ["select * from sys.fields  where schema_name=? and table_name=?" schema table]
        {:row-fn (fn [row] (vector (:fields_id row) (prepare-control row db)))
         :result-set-fn (fn [rs] (into {} rs))}))

     ;; get all controls that are in a select-result control
     (cond is_event
       (merge
        (certo.sql/select-sys-fields-sets-in-select-result-control-by-event-classes-id
         db
         {:event_classes_id table}
         {}
         {:row-fn
          #(merge-into-sys-fields db % is_result_view)
          :result-set-fn (fn [rs] (into {} rs))})

        ;; get all controls in sys.users
        (jdbc/query
         db
         ["select * from sys.fields where schema_name='sys' and table_name='users'"]
         {:row-fn (fn [row] (vector  (stf "sys" "rv_users" (:field_name row)) (prepare-control row db)))
          :result-set-fn (fn [rs] (into {} rs))}))

       ;; This case occurs when a select-result control appears in the search form at the top of a table view
       ;; TO DO: Currently this will throw and exception when
       ;; select_result_to_text is false.  Need to write code similar to
       ;; certo.sql/select-sys-fields-sets-in-select-result-control-by-schema-table but joining to find the controls that are in select-result controls that are in views.
       ;; The following provides a start:
       ;; select sfs.schema_name, sfs.table_name, sfs.field_name, sf.fields_id, sf.select_result_to_text from sys.field_sets as sfs inner join sys.fields sf on sfs.sys_fields_id=sf.fields_id where sf.control='select-result';
       is_view
       {}

       :else
       (certo.sql/select-sys-fields-sets-in-select-result-control-by-schema-table
        db
        {:schema schema :table table}
        {}
        {:row-fn
         #(merge-into-sys-fields db % is_result_view)
         :result-set-fn (fn [rs] (into {} rs))})))))


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
   "Since schema and table are not arguments to this function, it can be
   used to create the where clause for a view."
   (if (empty? params)
     [""]
     (let [operator (or (#{"and" "or"} (get params "operator"))
                        (throw (Exception. "Operator is invalid or not specified")))
           comparator (or (#{"beginning" "approximate" "exact"} (get params "comparator"))
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
                (if (and (or (= comparator "beginning") (= comparator "approximate")) (= (:type (get fields stf)) "text"))
                      (conj where-strings (str stf " ilike ?"))
                      (conj where-strings (str stf " = ?")))
                (cond (and (= comparator "beginning") (= (:type (get fields stf)) "text"))
                      (conj where-params (str value "%"))
                      (and (= comparator "approximate") (= (:type (get fields stf)) "text"))
                      (conj where-params (str "%" value "%"))
                      :else
                      (conj where-params value)))))))))))


(defn columns-clause
  ([fields schema table]
   (columns-clause fields schema table nil))
  ([fields schema table count-all-field-name]
   "This function does a lookup on schema and table in the fields
  hashmap.  Since no database query is run, and it throws an exception
  when no fields are found for the given schema and table, it avoids
  the risk of an SQL injection attack."
   (let [flds (fields-by-schema-table fields schema table)]
     (if (not (empty? flds))
       (format
        "select %s from %s.%s"
        (str
         (str/join
          ", "
          (concat
           (map
            #(str (str/join "." %) " as " "\"" (str/join "." %) "\"")
            (map
             #((juxt :schema_name :table_name :field_name) %)
             (map val flds)))
           (if count-all-field-name
             [(format "count(*) over() as %s" count-all-field-name)]
             []))))
        schema table)
       (throw (Exception. (format "No fields found for schema: %s and table: %s" schema table)))))))



(defn order-by-clause [fields {:strs [order-by direction] :as params}]
  (let [order-by
        (if (or (nil? order-by) ((set (map :field_name (vals fields))) order-by))
          order-by
          (throw (Exception. "Order by is invalid")))
        direction
        (if (or (nil? direction) (#{"asc" "desc"} direction))
          direction
          (throw (Exception. "Direction is invalid")))]
    (cond
      (and (not (nil? order-by)) (not (nil? direction)))
      (format "order by %s %s nulls last" order-by direction)
      (and (not (nil? order-by)) (nil? direction))
      (throw (Exception. "Direction not specified"))
      (and (nil? order-by) (not (nil? direction)))
      (throw (Exception. "Order by not specified"))
      :else
      "")))


(defn limit-offset-clause [limit offset]
  (let [limit (cu/parse-integer limit "Limit" pos?)
        limit-clause
        (if (nil? limit)
          ""
          (format "limit %d" limit))
        offset (cu/parse-integer offset "Offset" (fn [x] (>= x 0)))
        offset-clause
        (if (nil? offset)
          ""
          (format "offset %d" offset))]
    (str/trim (str limit-clause " " offset-clause))))


;; TO DO: Considering combining (select) and (select-by-id).

;; TO DO: Add the ability to specify an order by clause via query-params

;; TO DO: Store a default order by clause for each table in sys.tables.


(defn select-result-set
  ([db fields table-map schema table params count?]
   (select-result-set db fields table-map schema table params count? identity))
  ([db fields table-map schema table params count? row-fn]
   ;; TO DO: Get defaults preferably from config.
   (let [{:strs [limit offset direction] :or {limit "25" offset "" direction "asc"}} params
         [where-string & where-parameters] (where-clause fields (dissoc params "limit" "offset" "order-by" "direction") true)]
     (let [count-all (atom nil)
           count-all-field-name (keyword (str "count_all_" (str/replace (java.util.UUID/randomUUID) "-" "_")))
           rs
           (jdbc/query
            db
            (into
             (vector
              (str/join
               " "
               (filter
                #(not (str/blank? %))
                (vector
                 (columns-clause fields schema table (name count-all-field-name))
                 where-string
                 (order-by-clause fields (dissoc params "operator" "comparator" "limit" "offset"))
                 (limit-offset-clause limit offset)))))
             (or where-parameters []))
            {:row-fn
             (comp
              row-fn
              (fn [row]
                (reset! count-all (count-all-field-name row))
                (dissoc row count-all-field-name)))})]
       (if count?
         {:count (count rs)
          :count-all @count-all
          :result-set rs}
         ;; when count? is false just return the result set
         rs)))))


(defmulti select-mm (fn [db fields table-map schema table params count?] (st schema table)))


(defmethod select-mm "app.event_queue" [db fields table-map schema table params count?]
  (let [event-class-argument-dimensions (event-class-dimensions db :argument)]
    (select-result-set
     db fields table-map schema table params count?
     (fn [row]
       (assoc
        (dissoc row :app.event_queue.event_classes_id)
        :app.event_queue.event_queue_id
        ;; For example
        ;; {:event-queue-id 17
        ;; :event-classes-id "an-event-classes-id"
        ;; :event-class-argument-dimensions
        ;; {:app.event_queue.dimension_one_id 123, :app.event_queue.dimension_two_id nil, :app.event_queue.dimension_three_id 456}}
        {:event-queue-id (:app.event_queue.event_queue_id row)
         :event-classes-id (:app.event_queue.event_classes_id row)
         :event-class-argument-dimensions
         (into
          {}
          (map
           (fn [k]
             (vector k (get row (keyword (str "app.event_queue." (name k))))))
           (get event-class-argument-dimensions (:app.event_queue.event_classes_id row))))})))))


(defmethod select-mm :default [db fields table-map schema table params count?]
  (select-result-set db fields table-map schema table params count?))


(defn select
  ([db fields table-map schema table params]
   (select db fields table-map schema table params false))
  ([db fields table-map schema table params count?]
   (select-mm db fields table-map schema table params count?)))


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


(defn event! []
  )


(defn tracking-event! []
  )


(defn single-table-event! []
  )


;; TO DO: Consider doing something like this
;; (defn insert! ["event" "recruitment_coordinator_added_newmother"] [db md fields schema table params]
;;   (with-event [db md fields schema table params]
;;     (if (:is_event_done params)
;;       (sql-events/recruitment-participant-consented-mainstudy db params)
;;       (sql-events/recruitment-participant-did-not-consent-mainstudy db params))))


;; Dimensions are all integers (int8 foreign keys in Postgresql) and
;; are the result of query (not an HTTP request).  Therefore don't
;; need to use a prepared statement to protect against SQL injection
;; and just build the where clause as a string.
(defn event-class-dimensions-superset-where-clause [table ecrd]
  (let [ecrd (dissoc ecrd :events_id :created_by :created_at :updated_by :updated_at)
        ecrd (filter (fn [[k v]] (not (nil? v))) ecrd)]
    (if (empty? ecrd)
      ""
      (str
       "and ("
       (apply
        format
        ;; Using 'or' returns a superset of the desired rows.  This
        ;; superset will be filtered using an 'and' statement in a
        ;; filter.
        (str/join " or " (map (fn [[k v]] (str "aed." (name k) " = %d")) ecrd))
        (vals ecrd))
       ")"))))


(defn event-class-dimensions-conjunction [x ecds ecrd]
  (let [ecrd (dissoc ecrd :events_id :created_by :created_at :updated_by :updated_at)
        ecrd (filter (fn [[k v]] (ecds k)) ecrd)]
    (if (empty? ecrd)
      []
      (map (fn [[k v]] (= (k x) v)) ecrd))))


(defn enqueue-dequeue-event-classes [tx depends-on-event-classes-id event-class-dimensions ecrd action]
  (assert (or (= action :enqueue) (= action :dequeue))
          (format "enqueue-dequeue-event-classes action must be :enqueue or :dequeue"))
  (let [sys-event-class-dnfs-schema-table (format "sys.event_class_%s_dnfs" (name action))]
    (map
     :event_classes_id
     (filter
      (fn [{:keys [event_classes_id term degree] :as all}]
        (let [ecds (set (get event-class-dimensions event_classes_id))
              number-true
              (reduce
               +
               (map
                (fn [x]
                  (if (every?
                       true?
                       (conj
                        (event-class-dimensions-conjunction x ecds ecrd)
                        (= (:is_positive x) (:is_event_done x))))
                    1
                    0))
                (certo.sql/enqueue-dequeue-event-class-candidates
                 tx
                 {:sys-event-class-dnfs-schema-table sys-event-class-dnfs-schema-table
                  :event_classes_id event_classes_id
                  :term term
                  :event-class-dimensions-superset-where-clause (event-class-dimensions-superset-where-clause event_classes_id ecrd)})))]
          (= number-true degree)))
      (certo.sql/enqueue-dequeue-event-class-dependencies
       tx
       {:sys-event-class-dnfs-schema-table sys-event-class-dnfs-schema-table
        :depends-on-event-classes-id depends-on-event-classes-id})))))


;; TO DO: table is event_classes_id, so rename the table argument to be event-classes-id
(defn enqueue-events! [tx table params ecrd event-class-argument-dimensions]
  (doseq [ecid (enqueue-dequeue-event-classes tx table event-class-argument-dimensions ecrd :enqueue)]
    (do
      (let [eq
            (first
             (jdbc/insert!
              tx
              "sys.event_queue"
              {:event_classes_id ecid
               :is_queued true
               :lag_years 0 :lag_months 0 :lag_weeks 0 :lag_days 0 :lag_hours 0 :lag_minutes 0 :lag_seconds 0
               :created_by (:created_by params) :updated_by (:updated_by params)}
              {:return-keys true}))]
        (jdbc/insert!
         tx
         "app.event_queue_dimensions"
         ;; columns
         (vec
          (concat
           [:event_queue_id]
           (get event-class-argument-dimensions ecid)
           [:created_by :updated_by]))
         ;; values
         (vec
          (concat
           [(:event_queue_id eq)]
           (mapv (fn [k] (get ecrd k)) (get event-class-argument-dimensions ecid))
           [(:created_by params) (:updated_by params)]))))
      ;; logging uses agent to prevent blocking in transaction
      (log/info (str "  enqueue: " ecid "\n")))))


;; TO DO: table is event_classes_id, so rename the table argument to be event-classes-id
(defn dequeue-events! [tx table params ecrd event-class-argument-dimensions]
  (doseq [ecid (enqueue-dequeue-event-classes tx table event-class-argument-dimensions ecrd :dequeue)]
    (do
      (jdbc/update!
       tx
       "sys.event_queue"
       {:is_queued false}
       (if (= table ecid)
         ;; include the event_queue_id in the where clause when removing the event that was just done from the queue
         ["event_classes_id = ? and event_queue_id = ?" ecid (:event_queue_id params)]
         ["event_classes_id = ?" ecid]))
      ;; on delete cascade will delete the corresponding row in app.event_queue_dimensions
      ;; logging uses agent to prevent blocking in transaction
      (log/info (str "  dequeue: " ecid "\n")))))


;; TO DO: schema is always "event" and table is event_classes_id, so
;; remove the schema argument and rename the table argument to be
;; event-classes-id
(defn insert-event! [db md fields table-map schema table params event-class-fn]
  (jdbc/with-db-transaction [tx db]
    (let [params (cu/str-to-key-map (ui-to-db fields params))
          params
          (if (not (:is_negatable table-map))
            (assoc params
                   :is_event_done true
                   :event_not_done_reason nil)
            params)
          event-class-argument-dimensions (event-class-dimensions tx :argument)]
      (certo.sql/select-event-to-dequeue tx {:event_queue_id (:event_queue_id params)})
      ;; The last statement of every event function is
      ;; insert into app.event_dimensions (...) values (...) returning *;
      ;; and is stored in edrd.
      ;; event-class-result-dimensions
      (let [ecrd
            ;; Run the function for this event
            (event-class-fn
             tx
             (assoc params
                    :event_classes_id table
                    :event_data
                    (json/write-value-as-string params)))]
        ;; logging uses agent to prevent blocking in transaction
        (log/info (format "did: %s with event_queue_id = %s\n" table (:events_id ecrd)))
        (log/info (str "  dequeue: " table "\n"))
        (dequeue-events! tx table params ecrd event-class-argument-dimensions)
        (enqueue-events! tx table params ecrd event-class-argument-dimensions)
        ;; return the event_class_result_dimensions so that they can be used by do-insert-event!
        ecrd))))


;; Insert an event and event dimsensions using the event argument dimensions.
(defn default-event-class-fn [event-class-argument-dimensions db params]
  (let [event (certo.sql-events/insert-event db (assoc params :events_id (:event_queue_id params)))]
    ;; Use app.event_class_result_dimensions to construct statement to
    ;; insert into app.event_dimensions.  Note that the dimensions
    ;; that are the argument dimensions, not the result dimensions,
    ;; since that's all we have in this case.
    (let [rs
          (jdbc/insert!
           db
           "app.event_dimensions"
           (assoc
            (into
             {}
             (map
              (fn [event-class-argument-dimension]
                (vector
                 event-class-argument-dimension
                 (get params event-class-argument-dimension)))
              (get event-class-argument-dimensions (:event_classes_id params))))
            :events_id (:event_queue_id params)
            :created_by (:created_by params)
            :updated_by (:updated_by params))
           {:return-keys true})]
      (case (count (take 2 rs))
        0 (throw (Exception. "Error: Not inserted."))
        1 (first rs)
        (throw (Exception. "Warning: Unexpected result on insert."))))))


(defn insert-schema-table! [db md fields table-map schema table params]
  (let [rs
        (jdbc/insert!
         db
         (st schema table)
         (ui-to-db fields params))]
    (case (count (take 2 rs))
      0 (throw (Exception. "Error: Not inserted."))
      1 true
      (throw (Exception. "Warning: Unexpected result on insert.")))))


(defmulti insert! (fn [db md fields table-map schema table params] [schema table]))


(defmethod insert! :default [db md fields table-map schema table params]
  (if (= schema "event")
    (do
      (println (format "function %s not found, using default.\n" table))
      (log/warn (format "function %s not found, using default.\n" table))
      (let [event-class-argument-dimensions (event-class-dimensions db :argument)]
        (insert-event! db md fields table-map schema table params
                       (partial default-event-class-fn event-class-argument-dimensions))))
    ;; (throw (Exception. (format "Function %s not found." table)))
    (insert-schema-table! db md fields table-map schema table params)))


;; Use to programmatically insert events.  The params are of the form
;; that comes from the ui, i.e. string keys and string values.
;; Specifically, "" for nil, "true" for true, and "false" for false,
;; and strings for doubles.
(defn do-insert-event!
  ([system event-classes-id params]
   (do-insert-event! (get-in system [:database :db-spec]) (:metadata system) event-classes-id params))
  ([db md event-classes-id params]
   (let [table-map (event-classes db event-classes-id)
         fields (fields db "event" event-classes-id table-map)
         params (into {} (map (fn [[k v]] (vector (str "event." event-classes-id "." (name k)) v)) params))]
     (insert!
      db
      md
      fields
      table-map
      "event"
      event-classes-id
      params))))


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
        (jdbc/query
         db
         ["select schema_name from sys.tables group by schema_name"]
         {:row-fn :schema_name})]
    {:event-queue
     (jdbc/query
      db
      ;; TO DO: Use this after inactive is included in the sys.event_classes table
      ;; ["select sec.event_classes_id from sys.event_classes sec left outer join sys.event_class_enqueue_dnfs ecp on sec.event_classes_id=ecp.event_classes_id where ecp.event_classes_id is null and sec.inactive='false'"]
      ["select sec.event_classes_id from sys.event_classes sec left outer join sys.event_class_enqueue_dnfs ecp on sec.event_classes_id=ecp.event_classes_id where ecp.event_classes_id is null"])
     :sts
     (map
      (fn [schema]
        [schema
         (jdbc/query
          db
          ["select * from sys.tables where schema_name = ? order by table_type='table' desc, table_type='view' desc, table_type='option-table' desc, table_type='result-view' desc, table_name"  schema]
          {:row-fn #(assoc % :table (:table_name %) :count (select-count-star db (st schema (:table_name %))))})])
      schemas)}))


(defn user [db username]
  (first
   (jdbc/query
    db
    ["select su.*, sou.label as usergroup_label from sys.users as su inner join sys.ot_usergroups as sou on su.usergroup=sou.value where username=?"
     username])))

