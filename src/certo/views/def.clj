(ns certo.views.def
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [hiccup.core :as h]   
   [hiccup.form :as f]   
   [hiccup.page :as p]
   [hiccup.def :refer [defelem]]
   [java-time :as jt]
   [ring.util.response :as response]
   [ring.util.anti-forgery :as af]
   [ring.middleware.session :refer :all]
   [certo.models.default :as models]
   [certo.utilities :as u]
   [certo.views.common :as common]))


(def ^:const select-result-field-column-width 15)


(defelem date-field
  "Creates a new date input field."
  ([name] (date-field name nil))
  ([name value]
   (#'f/input-field "date" name value)))


(defelem datetime-field
  "Creates a new datetime input field. This control expects the following format 2017-06-01T14:30"
  ([name] (datetime-field name nil))
  ([name value] (#'f/input-field "datetime-local" name value)))


(defelem number-field
  "Creates a new number input field.  To restrict the input to
  integers, the caller includes the attribute {:step k} for some
  integer k, and to specify the number of digits after the decimal
  point input for a float, the caller includes the attribute {:step x}
  where x is 0.1, 0.01, etc."
  ([name] (number-field name nil))
  ([name value]
   (#'f/input-field "number" name value)))


(defelem timestamp-field
  "Creates a new timestamp input field."
  ([name] (timestamp-field name nil))
  ([name value] (f/text-field name value)))


(defelem select-boolean-field
  "Creates a new select-boolean input field."
  ([name options] (select-boolean-field name options nil))
  ([name options value]
   (f/drop-down
    name
    options
    value)))


(defn db-to-label [value]
  (if (not (nil? value))
    (condp = (class value)
      java.lang.Boolean value
      java.lang.Double value
      java.lang.Integer value
      java.lang.Long value
      java.lang.String value
      java.sql.Date (jt/format "MM/dd/yyyy" (jt/local-date value))
      java.sql.Timestamp (jt/format "MM/dd/yyyy, HH:mm:ss" (jt/local-date-time value))
      java.util.UUID value
      (throw (Exception. (format "Unknown %s for value %s" (class value) value))))
    ""))


;; TO DO: Do  something like this to organize a search box for the select-result-field
;; <table border="1">
;;   <tr> <td> Label 1 </td><td> Control 1 </td></tr>
;;   <tr>
;;     <td> Label 2 </td>
;;     <td>
;;     <div> Search </div>
;;     <div> Table </div>
;;     </td>
;;   </tr>
;;   <tr> <td> Label 3 </td><td> Control 3 </td></tr>
;; </table>


(defelem select-result-field
  "Creates a new select-result input field."
  ([name options] (select-result-field name options nil))
  ([name options value]
   ;; Cheap way to get a table to select or display data: use a select
   ;; control with a fixed width font and pad fields with non-breaking
   ;; spaces so they line up in columns.
   (f/drop-down
    name
    (map
     (fn [option]
       (vector
        (if (:value option)
          (map
           ;; TO DO: Should not be calling db-to-label.  Should call
           ;; db-to-table (since it depends on both type and control)
           ;; with a new argument that indicates should not return a
           ;; link, just a value.
           (fn [[k v]] (u/pads (str (db-to-label v)) select-result-field-column-width "&nbsp;" true))
           (:label-fields option))
          "")
        (:value option)))
     options)
    value)))

