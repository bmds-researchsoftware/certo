(ns certo.system
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as log]
   [certo.database :as db]
   [certo.metadata :as metadata]
   [certo.utilities :as u]   
   [certo.webapp :as webapp]))


(defn new-system [system-name handler]
  (let [config (u/config system-name)]
    (log/set-config! [:appenders :standard-out :enabled?] false)
    (log/set-config! [:appenders :spit :enabled?] true)
    (log/set-config! [:shared-appender-config :spit-filename] (:log-filename config))
    (log/set-config! [:appenders :spit :async?] true)
    (log/set-level! :debug)
    (component/system-map
     :database (db/new-database (:database config))
     :metadata (component/using
                (metadata/new-metadata (:metadata config) system-name)
                [:database])
     :webapp (component/using
              (webapp/new-webapp (:webapp config) handler)
              [:database :metadata]))))

