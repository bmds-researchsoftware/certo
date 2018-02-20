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

   [qualified.core :as q]))


;; ----- start: system -----
(alter-var-root #'*out* (constantly *out*))

(alter-var-root #'*err* (constantly *err*))

(def system nil)

(defn init [& args]
  (alter-var-root #'system
                  ;;(constantly (system/new-system system-name))
                  (constantly (system/new-system q/system-name q/wrapped-handler))))

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
  ;;(clojure.tools.namespace.repl/refresh-all :after 'user/go)
  (clojure.tools.namespace.repl/refresh :after 'user/go))
;; ----- end: system -----


;; ----- start: helpers -----
(defn- db []
  (get-in system [:database :db-spec]))

(defn- fields []
  (cmd/fields (db)))
;; ----- end: helpers -----

