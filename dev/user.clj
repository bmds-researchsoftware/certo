(ns user
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]
   [clojure.repl :refer :all]
   [clojure.string :as str]
   [clojure.test :as test]
   [eftest.runner :refer [find-tests run-tests]]
   [java-time :as jt]

   [certo.controllers.default :as ccd]
   [certo.middleware :as cm]
   [certo.models.default :as cmd]
   [certo.appsql :as appsql]
   [certo.sql :as sql]
   [certo.system :as system]
   [certo.utilities :as cu]
   [certo.views.default :as cvd]

   ;; these namespaces must be required, the others listed above are
   ;; included to support "repl-based" development
   [dev :refer :all]   
   [certoa.core :as certoa]))


;; ----- start: helpers -----
(defn- db []
  (get-in system [:database :db-spec]))

(defn- fields []
  (cmd/fields (db)))

(defn sys-hash-maps-to-db [db]
  (cu/hash-maps-to-db db "resources/db/sys-options-usergroups.clj" sql/insert-sys-options-usergroups)
  (cu/hash-maps-to-db db "resources/db/sys-users.clj" sql/insert-sys-users)
  (cu/hash-maps-to-db db "resources/db/sys-options-types.clj" sql/insert-sys-options-types)
  (cu/hash-maps-to-db db "resources/db/sys-options-controls.clj" sql/insert-sys-options-controls)
  (cu/hash-maps-to-db db "resources/db/sys-fields.clj" sql/insert-sys-fields)
  (cu/hash-maps-to-db db "resources/db/sys-event-classes.clj" sql/insert-sys-event-classes)
  (cu/hash-maps-to-db db "resources/db/sys-event-classes-fields.clj" sql/insert-sys-event-classes-fields))

(defn app-hash-maps-to-db [db]
  (cu/hash-maps-to-db db "resources/db/app-sys-fields.clj" sql/insert-sys-fields)  
  (cu/hash-maps-to-db db "resources/db/app-options-states.clj" appsql/insert-app-options-states)
  (cu/hash-maps-to-db db "resources/db/study-subjects.clj" appsql/insert-study-subjects)
  (cu/hash-maps-to-db db "resources/db/app-notes.clj" appsql/insert-app-notes))

(defn all-hash-maps-to-db [db]
  (sys-hash-maps-to-db db)
  (app-hash-maps-to-db db))
;; ----- end: helpers -----


(set-init-fn certoa/system-new-fn)

