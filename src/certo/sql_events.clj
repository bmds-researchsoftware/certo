(ns certo.sql-events
  (:require
   [hugsql.core :as hugsql]))


;; The path is relative to the classpath, not the project directory,
;; so the files referenced below are in
;; $PROJECT_DIRECTORY/resources/db/.

(hugsql/def-db-fns "db/sys-events.sql")

(hugsql/def-sqlvec-fns "db/sys-events.sql")

