(ns certo.core
  (:require
   [com.stuartsierra.component :as component]
   [certo.system :as system]
   [qualified.core :as q])
  (:gen-class))


(defn -main [& args]
  (let [system-name "certo"
        system (system/new-system q/system-name q/wrapped-handler)]
    (component/start system)))

