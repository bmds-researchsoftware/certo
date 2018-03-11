(ns certo.sql
  (:require [hugsql.core :as hugsql]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
               [certo.models.default :as models]
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


(def ordered-fields
  {;; select count (*) from app.notes;
   :app-notes ;; 1
   [:subjects_id :note :created_by :updated_by]

   ;; select count (*) from app.select_options;
   :app-select-options ;; 14
   [:schema_name :table_name :field_name :label :text_value :integer_value :location :created_by :updated_by]

   ;; select count (*) from study.subjects;
   :study-subjects ;; 7
   [:first_name :last_name :birth_date :birth_state :created_by :updated_by]

   ;; select count (*) from sys.event_classes;
   :sys-event-classes [:name :description :created_by :updated_by] ;; 1

   ;; select count (*) from sys.event_classes_fields;
   :sys-event-classes-fields [:event_classes_id :fields_id :location :created_by :updated_by] ;; 4

   ;; select count (*) from sys.events;
   :sys-events [] ;; 0

   ;; select count (*) from sys.events_queue;
   :sys-events-queue [] ;; 0

   ;; select count (*) from sys.fields;
   :sys-fields ;; 37
   [:schema_name :table_name :field_name :type :is_pk :label :control :location :in_table_view :disabled :readonly :required :text_max_length
    :date_min :date_max
    :integer_step :integer_min :integer_max
    :float_step :float_min :float_max
    :select_multiple :select_size
    :created_by :updated_by]

   ;; select count (*) from sys.notes;
   :sys-notes [] ;; 0

   ;; select count (*) from sys.select_options;
   :sys-select-options ;; 28
   [:schema_name :table_name :field_name :label :text_value :integer_value :location :created_by :updated_by]

   ;; select count (*) from sys.tables;
   :sys-tables [] ;; 0

   ;; select count (*) from sys.users;
   :sys-users ;; 2
   [:username :password :usergroup :created_by :updated_by]

   ;; select count (*) from val.controls;
   :val-controls ;; 9
   [:control :created_by :updated_by]   

   ;; select count (*) from val.types;    
   :val-types ;; 8
   [:type :created_by :updated_by]

   ;; select count (*) from val.usergroups;
   :val-usergroups ;; 4
   [:usergroup :created_by :updated_by]})


(defn db-to-hash-map [db schema table ordered-fields filename order-by]
  "Queries a database table and writes out a hash-map corresponding to
  each row in the table, and with the order of the field-value pairs
  of hash-map given by the order-fields argument."
  (let [select-statement (format "select * from %s" (models/st schema table))
        rows (jdbc/query db [(if order-by
                               (str select-statement " order by " order-by)
                               select-statement)])]
    (with-open [out (clojure.java.io/writer filename)]
      (doseq [row
              (map
               (fn [row]
                 (str
                  "{"
                  (str/join
                   " "
                   (map
                    (fn [ordered-field]
                      (let [ordered-val (get row ordered-field)
                            ordered-val
                            ;; drop reader literals
                            (if (or (instance? java.sql.Date ordered-val)
                                    (instance? java.sql.Timestamp ordered-val))
                              (str ordered-val)
                              ordered-val)]
                        (clojure.pprint/cl-format nil "~s ~s" ordered-field
                         (cond (= ordered-val "false") false
                               (= ordered-val "true") true
                               :else ordered-val))))
                    ordered-fields))
                  "}\n"))
               rows)]
        (.write out row)
        (.write out "\n")))))


(defn db-to-hash-maps [db]
  (db-to-hash-map db "app" "select_options" (:app-select-options ordered-fields) "/tmp/app-select-options.clj" "id")
  (db-to-hash-map db "app" "notes" (:app-notes ordered-fields) "/tmp/app-notes.clj" "id")  
  (db-to-hash-map db "study" "subjects" (:study-subjects ordered-fields) "/tmp/study-subjects.clj" "id")
  (db-to-hash-map db "sys" "event_classes" (:sys-event-classes ordered-fields) "/tmp/sys-event-classes.clj" "id")
  (db-to-hash-map db "sys" "event_classes_fields" (:sys-event-classes-fields ordered-fields) "/tmp/sys-event-classes-fields.clj" "id")
  (db-to-hash-map db "sys" "fields" (:sys-fields ordered-fields) "/tmp/sys-fields.clj" "schema_name asc, table_name asc, location asc")
  (db-to-hash-map db "sys" "select_options" (:sys-select-options ordered-fields) "/tmp/sys-select-options.clj" "id")
  (db-to-hash-map db "sys" "users" (:sys-users ordered-fields) "/tmp/sys-users.clj" "id")
  (db-to-hash-map db "val" "controls" (:val-controls ordered-fields) "/tmp/val-controls.clj" "id")
  (db-to-hash-map db "val" "types" (:val-types ordered-fields) "/tmp/val-types.clj" "id")
  (db-to-hash-map db "val" "usergroups" (:val-usergroups ordered-fields) "/tmp/val-usergroups.clj" "id"))


(defn hash-maps-to-db [db filename f]
  (with-open [r (clojure.java.io/reader filename)]
    (doseq [line (line-seq r)]
      (when (not= line "")
        (f db (clojure.edn/read-string line))))))


(defn hash-maps-to-db-sys [db]
  (hash-maps-to-db db "resources/db/sys-users.clj" insert-sys-users)
  (hash-maps-to-db db "resources/db/val-controls.clj" insert-val-controls)
  (hash-maps-to-db db "resources/db/val-types.clj" insert-val-types)
  (hash-maps-to-db db "resources/db/val-usergroups.clj" insert-val-usergroups)
  (hash-maps-to-db db "resources/db/sys-fields.clj" insert-sys-fields)
  (hash-maps-to-db db "resources/db/sys-event-classes.clj" insert-sys-event-classes)
  (hash-maps-to-db db "resources/db/sys-event-classes-fields.clj" insert-sys-event-classes-fields)
  (hash-maps-to-db db "resources/db/sys-select-options.clj" insert-sys-select-options))


(defn hash-maps-to-db-app [db]
  (hash-maps-to-db db "resources/db/app-select-options.clj" insert-app-select-options)
  (hash-maps-to-db db "resources/db/study-subjects.clj" insert-study-subjects)
  (hash-maps-to-db db "resources/db/app-notes.clj" insert-app-notes))


(defn hash-maps-to-db-all [db]
  (hash-maps-to-db-sys db)
  (hash-maps-to-db-app db))

