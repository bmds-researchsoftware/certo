(ns certo.webapp
  (:require
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :as jetty]))


(defrecord WebApp [owner]
  component/Lifecycle

  (start [component]
    (if (:http-server component)
      component
      (assoc component
             :http-server
             (jetty/run-jetty
              ((:handler component) (get-in component [:database :connection]) (:metadata component))
              {:join? false
               :ssl? (:ssl? component)
               :ssl-port (:ssl-port component)
               :keystore (:keystore component)
               :key-password (str/trim (slurp (:key-password-filename component)))
               :client-auth (:client-auth component)}))))
  
  (stop [component]
    (if-let [http-server (:http-server component)]
      (do (.stop http-server)
          (.join http-server)
          (dissoc component :http-server))
      component)))


(defn new-webapp [config handler]
  (map->WebApp (assoc config :handler handler)))

