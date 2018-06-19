(ns certo.sql
  (:require
   [hugsql.core :as hugsql]))


;; The path is relative to the classpath, not the project directory,
;; so the files referenced below are in
;; $PROJECT_DIRECTORY/resources/db/.

(hugsql/def-db-fns "db/sys.sql")

(hugsql/def-sqlvec-fns "db/sys.sql")

(defn insert-sys-tables-executive [db params]
  (case (:table_type params)
    "table" (insert-sys-tables db params)
    "view" (insert-sys-views db params)
    "result-view" (insert-sys-result-views db params)
    "option-table" (insert-sys-option-tables db params)
    (throw (Exception. (format "insert-sys-tables-executive:: invalid table_type %s" (:table_type params))))))
