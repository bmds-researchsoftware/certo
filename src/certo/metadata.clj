(ns certo.metadata
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :as jetty]
   [certo.models.default :as cmd]))


;; Returns something like "study/subjects|sys/fields|app/ot_states"
(defn- schema-tables [sts]
  (map (fn [[k v]] (str (:schema_name v) "/" (:table_name v))) sts))


;; "Returns something like event/completed_event_a|event/completed_event_b|event/completed_event_c"
(defn- event-classes [ecs]
  (map (fn [[k v]] (str "event/" (:event_classes_id v))) ecs))


(defrecord Metadata []
  component/Lifecycle

  (start [component]
    ;; if using connection or a connection-pool open it here and
    ;; assoc it to the component
    (if (:routes-regex component)
      component
      (assoc component
             :routes-regex
             (str/join
              "|"
              (concat
               (-> component
                   (get-in [:database :db-spec])
                   (cmd/tables)
                   (schema-tables))

               (-> component
                   (get-in [:database :db-spec])
                   (cmd/event-classes)
                   (event-classes))))
             :functions (cmd/functions (get-in component [:database :db-spec])))))

  (stop [component]
    ;; if using connection or a connection-pool close it here and
    ;; dissoc it with the component
    (dissoc component :routes-regex)))


(defn new-metadata [system-name]
  (map->Metadata {:system-name system-name}))

