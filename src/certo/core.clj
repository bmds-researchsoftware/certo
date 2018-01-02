(ns certo.core
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]   
   [compojure.core :refer [GET POST] :as compojure]
   [compojure.route :as route]   
   [ring.adapter.jetty :as jetty]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
   [clojure.java.jdbc :as jdbc]))


(defn pgpass-map [filename]
  (map
   (fn [line] (zipmap [:host :port :dbname :user :password] (str/split line  #":")))
   (str/split-lines (slurp filename))))


(defn get-dbspec [{:keys [:db-spec :pgpass]}]
  (let [keys [:host :port :dbname :user]]
    (merge
     db-spec
     (if-let [match
              (first
               (filter
                (fn [pgpass-map]
                  (= (select-keys pgpass-map keys)
                     (select-keys db-spec keys)))
                (pgpass-map pgpass)))]
       match
       (throw (Exception. (format "No matching password for %s" (select-keys db-spec keys))))))))


(def config
  (edn/read-string (slurp "resources/config.clj")))


;; The resulting db-spec should look like this:
;; (def db-spec
;;   {:dbtype "postgresql"
;;    :dbname "certo"
;;    :host "localhost"
;;    :user "certo"
;;    :password "PGPASSWORD"
;;    :ssl true
;;    :sslfactory "org.postgresql.ssl.NonValidatingFactory"})


(def db-spec (get-dbspec (:database config)))


(defn select-count-subjects [db]
  (jdbc/query
   db
   ["select count(*) as cnt from study.subjects"]
   {:row-fn :cnt :result-set-fn first}))


(defn select-count-subjects-by-last-name [db prefix]
  (jdbc/query
   db
   [(format "select count(*) as cnt from study.subjects where last_name like '%s%%'" (str/upper-case prefix))]
   {:row-fn :cnt :result-set-fn first}))


(defn handler [db]
  (compojure/routes
      
   (compojure/GET
    "/"
    request
    {:body  (format "There are %d subjects." (select-count-subjects db))
     :status 200
     :headers {"Content-Type" "text/plain"}})

   (compojure/GET
    "/:prefix"
    [prefix]
    {:body  (format "There are %d subjects whose last name begins with '%s'." (select-count-subjects-by-last-name db prefix) prefix)
     :status 200
     :headers {"Content-Type" "text/plain"}})
   
   (route/not-found "<h1>Page not found</h1>")))


;; (def wrapped-handler (wrap-defaults (handler db-spec) site-defaults))
(def wrapped-handler (-> (handler db-spec) (wrap-defaults site-defaults)))


(defonce http-server
  (jetty/run-jetty
   #'wrapped-handler
   {:join? false :port (get-in config [:http-server :port])}))

