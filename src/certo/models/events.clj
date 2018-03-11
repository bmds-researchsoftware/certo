(ns certo.models.events
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]   
   [clojure.java.jdbc :as jdbc]
   [clojure.pprint :as pprint]
   [certo.utilities :as cu])
  (:import [java.util.UUID]))

;; event_classes

;; event_classes_fields

;; events_queue

;; events


(defn fields-by-event-class [fields event-class-event-classes-id]

  )

(defn event-classes-fields [db]
  (jdbc/query
   db
   ["select f.*, ecf.location from sys.event_classes_fields ecf inner join sys.fields f on ecf.id=f.id where event_classes_id=1 order by ecf.location"]))
