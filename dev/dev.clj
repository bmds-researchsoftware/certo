(ns dev
  (:require
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :as component]
   [certo.utilities :as u]))


(alter-var-root #'*out* (constantly *out*))

(alter-var-root #'*err* (constantly *err*))

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
  (u/touch ["src/certo/sql.clj"])
  (u/touch ["src/certo/appsql.clj"])  
  ;; (clojure.tools.namespace.repl/refresh-all :after 'user/go)
  (clojure.tools.namespace.repl/refresh :after 'dev/go))

