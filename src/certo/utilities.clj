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


(defn touch [fs]
  (doseq [f fs]
    (.setLastModified
     (clojure.java.io/file f)
     (java-time/to-millis-from-epoch (java-time/instant)))))


(defn hash-maps-to-db [db filename f]
  (with-open [r (clojure.java.io/reader filename)]
    (doseq [line (line-seq r)]
      (when (not= line "")
        (f db (clojure.edn/read-string line))))))


;; Used in one time use function db-to-hash-maps
(defn db-to-hash-map [db schema table ordered-fields filename order-by]
  "Queries a database table and writes out a hash-map corresponding to
  each row in the table, and with the order of the field-value pairs
  of hash-map given by the order-fields argument."
  (let [select-statement (format "select * from %s" (str schema "." table))
        rows (jdbc/query db [(if order-by
                               (str select-statement " order by " order-by)
                               select-statement)])]
    (with-open [out (clojure.java.io/writer filename)]
      (doseq [row
              (map
               (fn [row]
                 (str
                  "{"
                  (str/join
                   " "
                   (map
                    (fn [ordered-field]
                      (let [ordered-val (get row ordered-field)
                            ordered-val
                            ;; drop reader literals
                            (if (or (instance? java.sql.Date ordered-val)
                                    (instance? java.sql.Timestamp ordered-val))
                              (str ordered-val)
                              ordered-val)]
                        (clojure.pprint/cl-format nil "~s ~s" ordered-field
                         (cond (= ordered-val "false") false
                               (= ordered-val "true") true
                               :else ordered-val))))
                    ordered-fields))
                  "}\n"))
               rows)]
        (.write out row)
        (.write out "\n")))))



;; (pads "The Clojure language" 10 "nbsp;" true) =>  "The Clo..."
;; (pads "The Clojure language" 10 "nbsp;" false) => "The Clojur"
;; (pads "The Clojure language" 25 "nbsp;" true) =>  "The Clojure languagenbsp;nbsp;nbsp;nbsp;nbsp;"
(defn pads [s k p ellipsis?]
  "Pad a string s with string p (optionally adding ellipsis) so that
  the length of the result is k."
  (let [cs (inc (count s))
        s (str s p)]
    (cond (> k cs)
          (str s (str/join (repeat (- k cs) p)))
          (< k cs)
          (if ellipsis?
            (str (subs s 0 (- k 4)) "..." p)
            (str (subs s 0 (dec k)) p))
          :else
          s)))

