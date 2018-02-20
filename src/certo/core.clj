(ns certo.core
  (:require
   [com.stuartsierra.component :as component]
   [certo.system :as system])
  (:gen-class))


(defn -main [& args]
  (let [system-name "certo"
        system (system/new-system system-name)]
    (component/start system)))

