(ns certo.system
  (:require
   [com.stuartsierra.component :as component]
   [certo.database :as db]
   [certo.metadata :as metadata]
   [certo.utilities :as u]   
   [certo.webapp :as webapp]))


(defn new-system [system-name handler]
  (let [config (u/config system-name)]
    (component/system-map
     :database (db/new-database (:database config))
     :metadata (component/using
                (metadata/new-metadata (:metadata config) system-name)
                [:database])
     :webapp (component/using
              (webapp/new-webapp (:webapp config) handler)
              [:database :metadata]))))

