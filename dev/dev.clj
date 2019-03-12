(ns dev
  (:require
   [clojure.java.io :as io]
   [clojure.core.server :as server]
   [clojure.string :as str]   
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [com.stuartsierra.component :as component]
   [clj-stacktrace.repl]
   [eftest.runner :as eftest]
   [java-time :as jt]
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

(defn touch [fs]
  (doseq [f fs]
    (let [af (clojure.java.io/file f)]
      (if (.exists af)
        (.setLastModified
         af
         (java-time/to-millis-from-epoch (java-time/instant)))
        (throw (Exception. (format "File %s not found" (.getCanonicalPath af))))))))

(defn reset []
  (stop)
  ;; touch *.clj files so hugsql reloads them, and changes in the
  ;; *.sql files they use are realized
  (touch ["checkouts/certo/src/certo/sql.clj"
            "checkouts/certo/src/certo/sql_events.clj"])
  (let [app-home (.getCanonicalPath (clojure.java.io/file "."))
        app-name (last (str/split app-home  #"[/]"))
        app-sql-file (str app-home "/src/" app-name "/sql.clj")
        app-sql-events-file (str app-home "/src/" app-name "/sql_events.clj")]
    (touch [app-sql-file
              app-sql-events-file]))
  ;; (clojure.tools.namespace.repl/refresh-all :after 'user/go)
  (clojure.tools.namespace.repl/refresh :after 'dev/go))

(defmacro duration-ms
  "Evaluates expr and returns the elapsed time in milliseconds."
  [expr]
  `(let [start# (. System (currentTimeMillis))
         ret# ~expr]
     (- (. System (currentTimeMillis)) start#)))

(defrecord Duration
    [hours minutes seconds total-milliseconds]
    Object
    (toString [d]
      (str (:hours d) " hours, " (:minutes d) " minutes, " (:seconds d) " seconds")))

;; (defmethod print-method Duration [d writer]
;;   (.write writer (str (:hours d) " hours, " (:minutes d) " minutes, " (:seconds d) " seconds")))

(defmacro duration
  "Evaluates expr and returns the time it took as a Duration record."
  [expr]
  `(let [milliseconds# (duration-ms ~expr)
         total-milliseconds# milliseconds#
         hours# (quot milliseconds# (* 60 60 1000))
         milliseconds# (- milliseconds# (* hours# 60 60 1000))
         minutes# (quot milliseconds# (* 60 1000))
         seconds# (/ (- milliseconds# (* minutes# 60 1000)) 1000.0)]
     (map->Duration {:total-milliseconds total-milliseconds#  :hours hours#  :minutes minutes# :seconds seconds#})))

(defn testit []
  (binding [clojure.test/*test-out* *out*] 
    (eftest/run-tests (eftest/find-tests "test"))))

