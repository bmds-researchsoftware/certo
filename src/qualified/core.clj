(ns qualified.core
  (:require
   [com.stuartsierra.component :as component]
   [compojure.core :refer [GET POST] :as compojure]   

   [certo.system :as system]
   [certo.controllers.default :as ccd]
   [certo.middleware :as cm])
  (:gen-class))


(defonce system-name "certo")


(defn handlerr [db md]
  (compojure/routes

   ;; (qualified.controllers.sample/sample-handler db)

   (compojure/GET
    "/hello"
    []
    {:body "hello"
     :status 200
     :headers {"Content-Type" "text/plain"}})
   
   (ccd/default-handler db md)))


(defn -main [& args]
  (let [system-name system-name
        system (system/new-system system-name (partial cm/wrapped-handler handlerr))]
    (component/start system)))

