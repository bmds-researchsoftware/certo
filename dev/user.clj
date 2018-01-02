(ns user
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.pprint :refer (pprint)]
   [clojure.repl :refer :all]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)]
   [certo.core]))


(defn stop []
  (.stop certo.core/http-server))


(defn reset []
  (stop)
  (clojure.tools.namespace.repl/refresh-all))

