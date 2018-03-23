(ns certoa.core
  (:require
   [com.stuartsierra.component :as component]
   [compojure.core :refer [GET POST] :as compojure]   

   [certo.system :as system]
   [certo.controllers.default :as ccd]
   [certo.middleware :as cm])
  (:gen-class))


(defn handler [db md]
  (compojure/routes

   ;; (certoa.controllers.sample/sample-handler db)

   (compojure/GET
    "/hello"
    []
    {:body "hello"
     :status 200
     :headers {"Content-Type" "text/plain"}})
   
   (ccd/default-handler db md)))


(defonce system-name "certoa")


(defonce system-new-fn (fn [] (system/new-system system-name (partial cm/wrapped-handler handler))))


(defn -main [& args]
  (let [system-name system-name
        system (system-new-fn)]
    (component/start system)))

