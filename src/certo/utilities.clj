(ns certo.utilities
  (:require
   [clojure.edn :as edn]   
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]   
   [clojure.string :as str]
   [java-time :as jt]))


(defn system-config-filename [system-name]
  (let [system-name (str/upper-case system-name)
        system-config-filename
        (or (System/getenv (format "%s_CONFIG" system-name)) "resources/config.clj")]
    (assert (.exists (io/as-file system-config-filename))
            (format "Environment variable %s_HOME is not set and resources/config.clj does not exist" system-name))
    system-config-filename))


(defn config [system-name]
  (let [system-config-filename (system-config-filename system-name)]
    (merge {:system-name system-name}
           {:system-config-filename system-config-filename}
           (edn/read-string (slurp system-config-filename)))))


(defn date-now []
  (jt/local-date))

