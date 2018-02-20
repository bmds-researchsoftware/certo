(ns certo.metadata
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :as jetty]
   [certo.models.default :as cmd]))


;; Returns something like "study/subjects|sys/fields|vals/controls|vals/types"
(defn routes-regex [fields]
  (str/join
   "|"
   (into #{} (map (fn [[k v]] (str (:schema_name v) "/" (:table_name v))) fields))))


(defrecord Metadata []
  component/Lifecycle

  (start [component]
    ;; if using connection or a connection-pool open it here and
    ;; assoc it to the component
    (if (:fields component)
      component
      (let [fields
            (-> (get-in component [:database :db-spec])
                (cmd/fields))]
        (-> component
            (assoc :fields fields
                   :routes-regex (routes-regex fields))))))
  
  (stop [component]
    ;; if using connection or a connection-pool close it here and
    ;; dissoc it with the component
    (dissoc component :fields)))


(defn new-metadata []
  (map->Metadata {}))

