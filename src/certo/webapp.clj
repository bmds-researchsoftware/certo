(ns certo.webapp
  (:require
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :as jetty]))


(defrecord WebApp [owner port]
  component/Lifecycle

  (start [component]
    (if (:http-server component)
      component
      (assoc component
             :http-server
             (jetty/run-jetty
              ((:handler component) (get-in component [:database :db-spec]) (:metadata component))
              {:join? false :port port}))))
  
  (stop [component]
    (if-let [http-server (:http-server component)]
      (do (.stop http-server)
          (.join http-server)
          (dissoc component :http-server))
      component)))


(defn new-webapp [config handler]
  (map->WebApp (assoc config :handler handler)))

