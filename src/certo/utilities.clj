(ns certo.utilities
  (:require
   [clojure.edn :as edn]   
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]   
   [clojure.string :as str]
   [java-time :as jt]))


(defn system-config-filename [system-name]
  (let [system-name (str/upper-case system-name)
        system-config-filename
        (or (System/getenv (format "%s_CONFIG" system-name)) "resources/config.clj")]
    (assert (.exists (io/as-file system-config-filename))
            (format "Environment variable %s_HOME is not set and resources/config.clj does not exist" system-name))
    system-config-filename))


(defn config [system-name]
  (let [system-config-filename (system-config-filename system-name)]
    (merge {:system-name system-name}
           {:system-config-filename system-config-filename}
           (edn/read-string (slurp system-config-filename)))))


(defn date-now []
  (jt/local-date))


(defn hash-maps-to-db [db filename f]
  (with-open [r (clojure.java.io/reader filename)]
    (doseq [line (line-seq r)]
      (when (not= line "")
        (f db (clojure.edn/read-string line))))))


;; Used in one time use function db-to-hash-maps
(defn db-to-hash-map [db schema table ordered-fields filename order-by]
  "Queries a database table and writes out a hash-map corresponding to
  each row in the table, and with the order of the field-value pairs
  of hash-map given by the order-fields argument."
  (let [select-statement (format "select * from %s" (str schema "." table))
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


;; Used in one time use function db-to-hash-maps
;; (def ordered-fields
;;   {;; select count (*) from app.notes;
;;    :app-notes ;; 1
;;    [:subjects_id :note :created_by :updated_by]

;;    ;; select count (*) from app.select_options;
;;    :app-select-options ;; 14
;;    [:schema_name :table_name :field_name :label :text_value :integer_value :location :created_by :updated_by]

;;    ;; select count (*) from study.subjects;
;;    :study-subjects ;; 7
;;    [:first_name :last_name :birth_date :birth_state :created_by :updated_by]

;;    ;; select count (*) from sys.event_classes;
;;    :sys-event-classes [:name :description :created_by :updated_by] ;; 1

;;    ;; select count (*) from sys.event_classes_fields;
;;    :sys-event-classes-fields [:event_classes_id :fields_id :location :created_by :updated_by] ;; 4

;;    ;; select count (*) from sys.events;
;;    :sys-events [] ;; 0

;;    ;; select count (*) from sys.events_queue;
;;    :sys-events-queue [] ;; 0

;;    ;; select count (*) from sys.fields;
;;    :sys-fields ;; 37
;;    [:schema_name :table_name :field_name :type :is_pk :label :control :location :in_table_view :disabled :readonly :required :text_max_length
;;     :date_min :date_max
;;     :integer_step :integer_min :integer_max
;;     :float_step :float_min :float_max
;;     :select_multiple :select_size
;;     :created_by :updated_by]

;;    ;; select count (*) from sys.notes;
;;    :sys-notes [] ;; 0

;;    ;; select count (*) from sys.select_options;
;;    :sys-select-options ;; 28
;;    [:schema_name :table_name :field_name :label :text_value :integer_value :location :created_by :updated_by]

;;    ;; select count (*) from sys.tables;
;;    :sys-tables [] ;; 0

;;    ;; select count (*) from sys.users;
;;    :sys-users ;; 2
;;    [:username :password :usergroup :created_by :updated_by]

;;    ;; select count (*) from val.controls;
;;    :val-controls ;; 9
;;    [:control :created_by :updated_by]   

;;    ;; select count (*) from val.types;    
;;    :val-types ;; 8
;;    [:type :created_by :updated_by]

;;    ;; select count (*) from val.usergroups;
;;    :val-usergroups ;; 4
;;    [:usergroup :created_by :updated_by]})


;; One time use to store data in databaes in *.clj files
;; (defn db-to-hash-maps [db]
;;   (db-to-hash-map db "app" "select_options" (:app-select-options ordered-fields) "/tmp/app-select-options.clj" "id")
;;   (db-to-hash-map db "app" "notes" (:app-notes ordered-fields) "/tmp/app-notes.clj" "id")  
;;   (db-to-hash-map db "study" "subjects" (:study-subjects ordered-fields) "/tmp/study-subjects.clj" "id")
;;   (db-to-hash-map db "sys" "event_classes" (:sys-event-classes ordered-fields) "/tmp/sys-event-classes.clj" "id")
;;   (db-to-hash-map db "sys" "event_classes_fields" (:sys-event-classes-fields ordered-fields) "/tmp/sys-event-classes-fields.clj" "id")
;;   (db-to-hash-map db "sys" "fields" (:sys-fields ordered-fields) "/tmp/sys-fields.clj" "schema_name asc, table_name asc, location asc")
;;   (db-to-hash-map db "sys" "select_options" (:sys-select-options ordered-fields) "/tmp/sys-select-options.clj" "id")
;;   (db-to-hash-map db "sys" "users" (:sys-users ordered-fields) "/tmp/sys-users.clj" "id")
;;   (db-to-hash-map db "val" "controls" (:val-controls ordered-fields) "/tmp/val-controls.clj" "id")
;;   (db-to-hash-map db "val" "types" (:val-types ordered-fields) "/tmp/val-types.clj" "id")
;;   (db-to-hash-map db "val" "usergroups" (:val-usergroups ordered-fields) "/tmp/val-usergroups.clj" "id"))

