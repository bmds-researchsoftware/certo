(ns certo.sql
  (:require [hugsql.core :as hugsql]))


;; The path is relative to the classpath (not proj dir!),
;; so "src" is not included in the path.
;; The same would apply if the sql was under "resources/..."
;; Also, notice the under_scored path compliant with
;; Clojure file paths for hyphenated namespaces
;; (hugsql/def-db-fns "tranquility/sql/queue.sql")
(hugsql/def-db-fns "db/sys.sql")
(hugsql/def-db-fns "db/app.sql")


;; For most HugSQL usage, you will not need the sqlvec functions.
;; However, sqlvec versions are useful during development and
;; for advanced usage with database functions.
;; (hugsql/def-sqlvec-fns "tranquility/sql/queue.sql")
(hugsql/def-sqlvec-fns "db/sys.sql")
(hugsql/def-sqlvec-fns "db/app.sql")

