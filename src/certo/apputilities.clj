(ns certo.apputilities
  (:require
   [certo.utilities :as cu]))


;; Used in one time use function db-to-hash-maps
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


;; One time use to store data in databaes in *.clj files
(defn db-to-hash-maps [db]
  (cu/db-to-hash-map db "app" "select_options" (:app-select-options ordered-fields) "/tmp/app-select-options.clj" "id")
  (cu/db-to-hash-map db "app" "notes" (:app-notes ordered-fields) "/tmp/app-notes.clj" "id")  
  (cu/db-to-hash-map db "study" "subjects" (:study-subjects ordered-fields) "/tmp/study-subjects.clj" "id")
  (cu/db-to-hash-map db "sys" "event_classes" (:sys-event-classes ordered-fields) "/tmp/sys-event-classes.clj" "id")
  (cu/db-to-hash-map db "sys" "event_classes_fields" (:sys-event-classes-fields ordered-fields) "/tmp/sys-event-classes-fields.clj" "id")
  (cu/db-to-hash-map db "sys" "fields" (:sys-fields ordered-fields) "/tmp/sys-fields.clj" "schema_name asc, table_name asc, location asc")
  (cu/db-to-hash-map db "sys" "select_options" (:sys-select-options ordered-fields) "/tmp/sys-select-options.clj" "id")
  (cu/db-to-hash-map db "sys" "users" (:sys-users ordered-fields) "/tmp/sys-users.clj" "id")
  (cu/db-to-hash-map db "val" "controls" (:val-controls ordered-fields) "/tmp/val-controls.clj" "id")
  (cu/db-to-hash-map db "val" "types" (:val-types ordered-fields) "/tmp/val-types.clj" "id")
  (cu/db-to-hash-map db "val" "usergroups" (:val-usergroups ordered-fields) "/tmp/val-usergroups.clj" "id"))

