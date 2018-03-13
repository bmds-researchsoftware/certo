(ns user
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]
   [clojure.repl :refer :all]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :as component]   
   [eftest.runner :refer [find-tests run-tests]]
   [java-time :as jt]

   [certo.controllers.default :as ccd]
   [certo.models.default :as cmd]
   [certo.system :as system]
   [certo.utilities :as cu]
   [certo.views.default :as cvd]
   [certo.sql :as sql]
   [certo.utilities :as u]   

   [qualified.core :as q]))


;; ----- start: system -----
(alter-var-root #'*out* (constantly *out*))

(alter-var-root #'*err* (constantly *err*))

(def system nil)

(defn init [& args]
  (alter-var-root #'system
                  ;;(constantly (system/new-system system-name))
                  (constantly (system/new-system q/system-name q/wrapped-handler))))

(defn touch [fs]
  (doseq [f fs]
    (.setLastModified
     (clojure.java.io/file f)
     (java-time/to-millis-from-epoch (java-time/instant)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  ;; touch *.clj files so hugsql reloads them and changes in the *.sql
  ;; files they use are realized  
  (touch ["src/certo/sql.clj"])
  ;;(clojure.tools.namespace.repl/refresh-all :after 'user/go)
  (clojure.tools.namespace.repl/refresh :after 'user/go))
;; ----- end: system -----


;; ----- start: helpers -----
(defn- db []
  (get-in system [:database :db-spec]))

(defn- fields []
  (cmd/fields (db)))

(defn hash-maps-to-db-sys [db]
  (u/hash-maps-to-db db "resources/db/sys-options-usergroups.clj" sql/insert-sys-options-usergroups)
  (u/hash-maps-to-db db "resources/db/sys-users.clj" sql/insert-sys-users)
  (u/hash-maps-to-db db "resources/db/sys-options-types.clj" sql/insert-sys-options-types)
  (u/hash-maps-to-db db "resources/db/sys-options-controls.clj" sql/insert-sys-options-controls)
  (u/hash-maps-to-db db "resources/db/sys-fields.clj" sql/insert-sys-fields)
  (u/hash-maps-to-db db "resources/db/sys-event-classes.clj" sql/insert-sys-event-classes)
  (u/hash-maps-to-db db "resources/db/sys-event-classes-fields.clj" sql/insert-sys-event-classes-fields))

(defn hash-maps-to-db-app [db]
  (u/hash-maps-to-db db "resources/db/app-sys-fields.clj" sql/insert-sys-fields)  
  (u/hash-maps-to-db db "resources/db/app-options-states.clj" sql/insert-app-options-states)
  (u/hash-maps-to-db db "resources/db/study-subjects.clj" sql/insert-study-subjects)
  (u/hash-maps-to-db db "resources/db/app-notes.clj" sql/insert-app-notes))

(defn hash-maps-to-db-all [db]
  (hash-maps-to-db-sys db)
  (hash-maps-to-db-app db))
;; ----- end: helpers -----

