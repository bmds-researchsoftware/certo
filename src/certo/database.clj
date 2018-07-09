(ns certo.database
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [com.stuartsierra.component :as component]))


(defn pgpass-map [filename]
  (map 
   (fn [line] (zipmap [:host :port :dbname :user :password] (str/split line  #":")))
   (str/split-lines (slurp filename))))


;; The resulting db-spec should look like this:
;; (def db-spec
;;   {:dbtype "postgresql"
;;    :dbname "certo"
;;    :host "localhost"
;;    :user "certo"
;;    :password "PGPASSWORD"
;;    :ssl true
;;    :sslfactory "org.postgresql.ssl.NonValidatingFactory"})
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


(defrecord Database [db-spec]
  ;; [db-spec connection] <- use this if using connection
  ;; [db-spec connection-pool] <- use this if using connection-pool

  component/Lifecycle
  
  (start [component]
    ;; if using connection or a connection-pool open it here and
    ;; assoc it to the component, otherwise just return component
    (if (:connection component)
      component
      ;; (assoc component :connection (make-datasource (:db-spec component)))
      (assoc component :connection (:db-spec component))))

  (stop [component]
    ;; if using connection or a connection-pool close it here and
    ;; dissoc it with the component, otherwise just return component
    (if (:connection component)
      (do
        ;; (close-datasource (:connection component))
        (dissoc component :connection))
      component)))


(defn new-database [config]
  (map->Database {:db-spec (get-dbspec config)}))

