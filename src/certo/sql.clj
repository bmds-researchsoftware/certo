(ns certo.sql
  (:require [hugsql.core :as hugsql]
            [clojure.string :as str]))


;; The path is relative to the classpath (not proj dir!),
;; so "src" is not included in the path.
;; The same would apply if the sql was under "resources/..."
;; Also, notice the under_scored path compliant with
;; Clojure file paths for hyphenated namespaces
;; (hugsql/def-db-fns "tranquility/sql/queue.sql")
(hugsql/def-db-fns "db/sys.sql")
(hugsql/def-db-fns "db/app.sql")


;; For most HugSQL usage, you will not need the sqlvec functions.
;; However, sqlvec versions are useful during development and
;; for advanced usage with database functions.
;; (hugsql/def-sqlvec-fns "tranquility/sql/queue.sql")
(hugsql/def-sqlvec-fns "db/sys.sql")
(hugsql/def-sqlvec-fns "db/app.sql")


(def flds
  [
   ;; -- :name insert-sys-fields-sys-fields
   ;; -- :command :execute
   ;; -- :result :raw
   ;; -- :doc Insert into sys.fields for sys.fields
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "id", "serial8", "true", "ID", "integer", 0, "true", "false", "true", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "schema_name", "text", "false", "Schema Name", "text", 1, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "table_name", "text", "false", "Table Name", "text", 2, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "field_name", "text", "false", "Field Name", "text", 3, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "type", "text", "false", "Type", "select", 4, "true", "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "is_pk", "boolean", "false", "Is Primary Key?", "yes-no", 5, "false", "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "label", "text", "false", "Label", "text", 6, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "control", "text", "false", "Control", "select", 7, "false", "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :integer_step, :integer_min, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "position", "int8", "false", "Position", "integer", 8, "false",  1, 0, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "in_table_view", "boolean", "false", "In Table View?", "yes-no", 9, "false", "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :integer_step, :integer_min, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "text_max_length", "int8", "false", "Text Max Length", "integer", 10, "false",  1, 1, "false", "false", "false","root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "date_min", "date", "false", "Date Min", "date", 11, "false", "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "date_max", "date", "false", "Date Max", "date", 12, "false", "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :integer_step, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "integer_step", "int8", "false", "Integer Step", "integer", 13, "false",  1, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :integer_step, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "integer_min", "int8", "false", "Integer Min", "integer", 14, "false",  1, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :integer_step, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "integer_max", "int8", "false", "Integer Max", "integer", 15, "false",  1, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :float_step, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "float_step", "float8", "false", "Float Step", "float", 16, "false", 0.0000000001, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :float_min, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "float_min", "float8", "false", "Float Min", "float", 17, "false",  -1E-307, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :float_max, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "float_max", "float8", "false", "Float Max", "float", 18, "false",  1E+308, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "select_multiple", "boolean", "false", "Select Multiple?", "yes-no", 19, "false", "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :integer_step, :integer_min, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "select_size", "int8", "false", "Select Size", "integer", 20, "false", 1, 0, "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "disabled", "boolean", "false", "Disabled?", "yes-no", 21, "false", "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "readonly", "boolean", "false", "Readonly?", "yes-no", 22, "false", "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "fields", "required", "boolean", "false", "Required?", "yes-no", 23, "false", "false", "false", "true", "root", "root"]]


   ;; -- :name insert-sys-fields-study-subjects
   ;; -- :command :execute
   ;; -- :result :raw
   ;; -- :doc Insert into sys.fields for study.subjects
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["study", "subjects", "id", "serial8", "true", "ID", "integer", 0, "true", "false", "true", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["study", "subjects", "first_name", "text", "false", "First Name", "text", 1, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["study", "subjects", "last_name", "text", "false", "Last Name", "text", 2, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :date_min, :date_max, :disabled, :readonly, :required, :created_by, :updated_by] ["study", "subjects", "birth_date", "date", "false", "Birth Date", "date", 3, "true", "1700-01-01", "2025-12-31", "false", "false", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["study", "subjects", "birth_state", "text", "false", "Birth State", "select", 4, "false", "false", "false", "false", "root", "root"]]


   ;; -- :name insert-sys-fields-val-controls
   ;; -- :command :execute
   ;; -- :result :raw
   ;; -- :doc for val.controls
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["val", "controls", "id", "uuid", "true", "ID", "text", 0, "true", "false", "true", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["val", "controls", "control", "text", "false", "Control", "text", 1, "true", 25, "false", "false", "true", "root", "root"]]


   ;; -- :name insert-sys-fields-val-types
   ;; -- :command :execute
   ;; -- :result :raw
   ;; -- :doc for val.types
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["val", "types", "id", "uuid", "true", "ID", "text", 0, "true", "false", "true", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["val", "types", "type", "text", "false", "Type", "text", 1, "true", 25, "false", "false", "true", "root", "root"]]


   ;; -- :name insert-sys-fields-sys-users
   ;; -- :command :execute
   ;; -- :result :raw
   ;; -- :doc Insert into sys.fields for sys.users
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "users", "id", "uuid", "true", "ID", "text", 0, "true", "false", "true", "false", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "users", "username", "text", "false", "Username", "text", 1, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :text_max_length, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "users", "password", "text", "false", "Password", "text", 2, "true", 25, "false", "false", "true", "root", "root"]]
   [[:schema_name, :table_name, :field_name, :type, :is_pk, :label, :control, :position, :in_table_view, :disabled, :readonly, :required, :created_by, :updated_by] ["sys", "users", "usertype", "text", "false", "Usertype", "select", 3, "true", "false", "false", "true", "root", "root"]]])



(def ordered-flds
  ;; [;;:id
  ;;  :schema_name
  ;;  :table_name
  ;;  :field_name
  ;;  :type
  ;;  :is_pk
  ;;  :label
  ;;  :control
  ;;  :position
  ;;  :in_table_view
  ;;  :disabled
  ;;  :readonly
  ;;  :required

  ;;  :text_max_length
   
  ;;  :date_min
  ;;  :date_max
   
  ;;  :integer_step
  ;;  :integer_min
  ;;  :integer_max
   
  ;;  :float_step
  ;;  :float_min
  ;;  :float_max
   
  ;;  :select_multiple
  ;;  :select_size

  ;;  :created_by
  ;;  ;;:created_at
  ;;  :updated_by
  ;;  ;;:updated_at
  ;;  ]  
  [;;:id
   :schema_name
   :table_name
   :field_name
   :type
   :is_pk
   :required
   :created_by
   ;; :created_at
   :updated_by
   ;; :updated_at
   
   :label
   :control
   :position
   :in_table_view
   :disabled
   :readonly

   :text_max_length
   
   :date_min
   :date_max
   
   :integer_step
   :integer_min
   :integer_max
   
   :float_step
   :float_min
   :float_max
   
   :select_multiple
   :select_size
   ]

  )


(defn format-flds-clj []
  (doseq [row
          (map
           (fn [r]
             (merge (zipmap ordered-flds (repeat nil)) r))
           (map
            (fn [[es vs]]
              (into {} (map (fn [e v] [e v]) es vs)))
            flds))]
    (println "(insert-sys-fields db {")    
    (doseq [ordered-fld ordered-flds
            :let [ordered-val (get row ordered-fld)]]
      (println
       (clojure.pprint/cl-format nil "~s ~s" ordered-fld
                                 (cond (= ordered-val "false") false
                                       (= ordered-val "true") true
                                       :else ordered-val))))
    (println "})")
    (println)))


(defn format-flds-sql []
  (doseq [row
          (map
           (fn [r]
             (merge (zipmap ordered-flds (repeat nil)) r))
           (map
            (fn [[es vs]]
              (into {} (map (fn [e v] [e v]) es vs)))
            flds))]
    (println "insert into sys.fields db")
    (println (str (str/join "," ordered-flds) " values"))
    (doseq [ordered-fld ordered-flds
            :let [ordered-val (get row ordered-fld)]]
      (print
       (clojure.pprint/cl-format nil "~s, "
                                 (cond (= ordered-val "false") false
                                       (= ordered-val "true") true
                                       :else ordered-val))))
    (println ")")
    (println)))

(defn insert-sys-fields-sys-fields [db]
  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "id"
    :type "serial8"
    :is_pk true
    :label "ID"
    :control "integer"
    :position 0
    :in_table_view true
    :disabled false
    :readonly true
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "schema_name"
    :type "text"
    :is_pk false
    :label "Schema Name"
    :control "text"
    :position 1
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "table_name"
    :type "text"
    :is_pk false
    :label "Table Name"
    :control "text"
    :position 2
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "field_name"
    :type "text"
    :is_pk false
    :label "Field Name"
    :control "text"
    :position 3
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "type"
    :type "text"
    :is_pk false
    :label "Type"
    :control "select"
    :position 4
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "is_pk"
    :type "boolean"
    :is_pk false
    :label "Is Primary Key?"
    :control "yes-no"
    :position 5
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "label"
    :type "text"
    :is_pk false
    :label "Label"
    :control "text"
    :position 6
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "control"
    :type "text"
    :is_pk false
    :label "Control"
    :control "select"
    :position 7
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "position"
    :type "int8"
    :is_pk false
    :label "Position"
    :control "integer"
    :position 8
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step 1
    :integer_min 0
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "in_table_view"
    :type "boolean"
    :is_pk false
    :label "In Table View?"
    :control "yes-no"
    :position 9
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "text_max_length"
    :type "int8"
    :is_pk false
    :label "Text Max Length"
    :control "integer"
    :position 10
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step 1
    :integer_min 1
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "date_min"
    :type "date"
    :is_pk false
    :label "Date Min"
    :control "date"
    :position 11
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "date_max"
    :type "date"
    :is_pk false
    :label "Date Max"
    :control "date"
    :position 12
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "integer_step"
    :type "int8"
    :is_pk false
    :label "Integer Step"
    :control "integer"
    :position 13
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step 1
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "integer_min"
    :type "int8"
    :is_pk false
    :label "Integer Min"
    :control "integer"
    :position 14
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step 1
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "integer_max"
    :type "int8"
    :is_pk false
    :label "Integer Max"
    :control "integer"
    :position 15
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step 1
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "float_step"
    :type "float8"
    :is_pk false
    :label "Float Step"
    :control "float"
    :position 16
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step 1.0E-10
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "float_min"
    :type "float8"
    :is_pk false
    :label "Float Min"
    :control "float"
    :position 17
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min -1.0E-307
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "float_max"
    :type "float8"
    :is_pk false
    :label "Float Max"
    :control "float"
    :position 18
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max 1.0E308
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "select_multiple"
    :type "boolean"
    :is_pk false
    :label "Select Multiple?"
    :control "yes-no"
    :position 19
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "select_size"
    :type "int8"
    :is_pk false
    :label "Select Size"
    :control "integer"
    :position 20
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step 1
    :integer_min 0
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "disabled"
    :type "boolean"
    :is_pk false
    :label "Disabled?"
    :control "yes-no"
    :position 21
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "readonly"
    :type "boolean"
    :is_pk false
    :label "Readonly?"
    :control "yes-no"
    :position 22
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "fields"
    :field_name "required"
    :type "boolean"
    :is_pk false
    :label "Required?"
    :control "yes-no"
    :position 23
    :in_table_view false
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"}))




(defn insert-sys-fields-val-controls [db]
  (insert-sys-fields
   db
   {:schema_name "val"
    :table_name "controls"
    :field_name "id"
    :type "uuid"
    :is_pk true
    :label "ID"
    :control "text"
    :position 0
    :in_table_view true
    :disabled false
    :readonly true
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "val"
    :table_name "controls"
    :field_name "control"
    :type "text"
    :is_pk false
    :label "Control"
    :control "text"
    :position 1
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"}))


(defn insert-sys-fields-val-types [db]
    (insert-sys-fields
     db
     {:schema_name "val"
      :table_name "types"
      :field_name "id"
      :type "uuid"
      :is_pk true
      :label "ID"
      :control "text"
      :position 0
      :in_table_view true
      :disabled false
      :readonly true
      :required false
      :text_max_length nil
      :date_min nil
      :date_max nil
      :integer_step nil
      :integer_min nil
      :integer_max nil
      :float_step nil
      :float_min nil
      :float_max nil
      :select_multiple nil
      :select_size nil
      :created_by "root"
      :updated_by "root"})

    (insert-sys-fields
     db
     {:schema_name "val"
      :table_name "types"
      :field_name "type"
      :type "text"
      :is_pk false
      :label "Type"
      :control "text"
      :position 1
      :in_table_view true
      :disabled false
      :readonly false
      :required true
      :text_max_length 25
      :date_min nil
      :date_max nil
      :integer_step nil
      :integer_min nil
      :integer_max nil
      :float_step nil
      :float_min nil
      :float_max nil
      :select_multiple nil
      :select_size nil
      :created_by "root"
      :updated_by "root"}))


(defn insert-sys-fields-sys-users [db]
  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "users"
    :field_name "id"
    :type "uuid"
    :is_pk true
    :label "ID"
    :control "text"
    :position 0
    :in_table_view true
    :disabled false
    :readonly true
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "users"
    :field_name "username"
    :type "text"
    :is_pk false
    :label "Username"
    :control "text"
    :position 1
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "users"
    :field_name "password"
    :type "text"
    :is_pk false
    :label "Password"
    :control "text"
    :position 2
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "sys"
    :table_name "users"
    :field_name "usertype"
    :type "text"
    :is_pk false
    :label "Usertype"
    :control "select"
    :position 3
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"}))
  
(defn insert-sys-fields-study-subjects [db]
  (insert-sys-fields
   db
   {:schema_name "study"
    :table_name "subjects"
    :field_name "id"
    :type "serial8"
    :is_pk true
    :label "ID"
    :control "integer"
    :position 0
    :in_table_view true
    :disabled false
    :readonly true
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "study"
    :table_name "subjects"
    :field_name "first_name"
    :type "text"
    :is_pk false
    :label "First Name"
    :control "text"
    :position 1
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "study"
    :table_name "subjects"
    :field_name "last_name"
    :type "text"
    :is_pk false
    :label "Last Name"
    :control "text"
    :position 2
    :in_table_view true
    :disabled false
    :readonly false
    :required true
    :text_max_length 25
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "study"
    :table_name "subjects"
    :field_name "birth_date"
    :type "date"
    :is_pk false
    :label "Birth Date"
    :control "date"
    :position 3
    :in_table_view true
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min "1700-01-01"
    :date_max "2025-12-31"
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"})

  (insert-sys-fields
   db
   {:schema_name "study"
    :table_name "subjects"
    :field_name "birth_state"
    :type "text"
    :is_pk false
    :label "Birth State"
    :control "select"
    :position 4
    :in_table_view false
    :disabled false
    :readonly false
    :required false
    :text_max_length nil
    :date_min nil
    :date_max nil
    :integer_step nil
    :integer_min nil
    :integer_max nil
    :float_step nil
    :float_min nil
    :float_max nil
    :select_multiple nil
    :select_size nil
    :created_by "root"
    :updated_by "root"}))

