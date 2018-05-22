(ns certo.metadata
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :as jetty]
   [certo.models.default :as cmd]))


;; Returns something like "study/subjects|sys/fields|vals/controls|vals/types"
(defn routes-regex [tables]
  (str/join
   "|"
   (into #{} (map (fn [[k v]] (str (:schema_name v) "/" (:table_name v))) tables))))


(defrecord Metadata []
  component/Lifecycle

  (start [component]
    ;; if using connection or a connection-pool open it here and
    ;; assoc it to the component
    (if (:routes-regex component)
      component
      (assoc component
             :routes-regex
             (-> component
                 (get-in [:database :db-spec])
                 (cmd/tables)
                 (routes-regex)))))

  (stop [component]
    ;; if using connection or a connection-pool close it here and
    ;; dissoc it with the component
    (dissoc component :routes-regex)))


(defn new-metadata [system-name]
  (map->Metadata {:system-name system-name}))

