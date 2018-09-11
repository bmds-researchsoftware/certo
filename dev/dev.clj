(ns dev
  (:require
   [clojure.string :as str]   
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :as component]
   [certo.utilities :as u]))


(alter-var-root #'*out* (constantly *out*))

(alter-var-root #'*err* (constantly *err*))

(def app-home nil)

(def init-fn nil)

(def system nil)

(defn set-init-fn [the-init-fn]
  (alter-var-root
   #'init-fn
   (constantly the-init-fn)))

(defn init [& args]
  (alter-var-root
   #'system
   (constantly (init-fn))))

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
  ;; touch *.clj files so hugsql reloads them, and changes in the
  ;; *.sql files they use are realized
  (u/touch ["checkouts/certo/src/certo/sql.clj"
            "checkouts/certo/src/certo/sql_events.clj"])
  (let [app-home (.getCanonicalPath (clojure.java.io/file "."))
        app-name (last (str/split app-home  #"[/]"))
        app-sql-file (str app-home "/src/" app-name "/sql.clj")
        app-sql-events-file (str app-home "/src/" app-name "/sql_events.clj")]
    (u/touch [app-sql-file
              app-sql-events-file]))
  ;; (clojure.tools.namespace.repl/refresh-all :after 'user/go)
  (clojure.tools.namespace.repl/refresh :after 'dev/go))

