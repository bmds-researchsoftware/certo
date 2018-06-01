(ns certo.views.form
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
   [certo.views.common :as common]
   [certo.views.def :as cf]))


;; ----- start: db-to-form -----
(defmulti db-to-form (fn [{:keys [:type :control]} value] [(keyword type) (keyword control)]))


(defmethod db-to-form [:boolean :select-boolean] [field value]
  (cond
    (nil? value) nil
    (true? value) "true"
    (false? value) "false"
    :else
    (throw (Exception. (format "Invalid boolean value %s of class %s is nil? %s" value (class value) (nil? value))))))


(defmethod db-to-form [:timestamptz :datetime] [field value]
  "yyyy-MM-ddThh:mm"
  (when value
    (str (jt/format "yyyy-MM-dd" (jt/local-date value)) "T" (jt/format "HH:mm" (jt/local-time value)))))


(defmethod db-to-form [:timestamptz :timestamp] [field value]
  (when value
    (jt/format "MM/dd/yyyy, HH:mm:ss" (jt/local-date-time value))))


(defmethod db-to-form :default [field value]
  ;; (throw (Exception. (format "db-to-form::invalid type: %s for %s" (:type field) (:field_id field))))
  value)
;; ----- end: db-to-form -----


;; ----- start: form-field -----
(defn update-attrs [attrs field new-attrs-map]
  (into
   attrs
   (filter
    identity
    (map
     (fn [[in out]] (if (get field in) [out (get field in)] false))
     new-attrs-map))))


(defmulti form-field (fn [{:keys [:control]} name attrs value] [(keyword control)]))


(defmethod form-field [:date] [field name attrs value]
  (cf/date-field (update-attrs attrs field {:date_min :min :date_max :max}) name value))


(defmethod form-field [:datetime] [field name attrs value]
   (cf/datetime-field attrs name value))


(defmethod form-field [:float] [field name attrs value]
  ;; (number-field (assoc attrs :step 0.0000000001) name value)
  (cf/number-field (update-attrs attrs field {:float_step :step :float_min :min :float_max :max}) name value))


(defmethod form-field [:integer] [field name attrs value]
  ;; (number-field (assoc attrs :step 1) name value)
  ;; TO DO: 6em will allow a 6 digit integer to be input.
  ;; This should be computed from the integer_max and integer_min.
  (cf/number-field (update-attrs (assoc attrs :style "width: 6em;") field {:integer_step :step :integer_min :min :integer_max :max}) name value))


;; (defmethod form-field [:integer-key] [field name attrs value]
;;   (cf/number-field attrs name value))


(defmethod form-field [:select-boolean] [field name attrs value]
  (let [options (map vector
                     ["" (:boolean_true field) (:boolean_false field)]
                     [nil "true" "false"])]
    (cf/select-boolean-field attrs name options value)))


(defmethod form-field [:select-option] [field name attrs value]
  (f/drop-down (update-attrs attrs field {:select_multiple :multiple :select_size :size}) name (conj (:options field) ["" nil]) value))


(defmethod form-field [:select-result] [field name attrs value]
  (cf/select-result-field (assoc (update-attrs attrs field {:select_size :size}) :class "sr") name (conj (:options field) ["" nil]) value))


(defmethod form-field [:text] [field name attrs value]
  (f/text-field (update-attrs attrs field {:text_max_length :maxlength :size :size}) name value))


(defmethod form-field [:textarea] [field name attrs value]
  (f/text-area (update-attrs attrs field {:text_max_length :maxlength :textarea_cols :cols :textarea_rows :rows}) name value))


;; (defmethod form-field [:text-key] [field name attrs value]
;;   (f/text-field attrs name value))


(defmethod form-field [:timestamp] [field name attrs value]
  (cf/timestamp-field (update-attrs attrs field {:size :size}) name value))


(defmethod form-field :default [field name attrs value]
  (throw (Exception. (format "form-field::invalid control: %s" (:control field)))))
;; ----- end: form-field -----


(defn form-label [name field]
  (if (:required field)
    (f/label name (str (:label field) "*"))
    (f/label name (:label field))))


(defn form [fields schema table action data]
  (let [fields (models/sort-by-location fields)
        stfs (map key fields)
        ;; there should be exactly one id field
        id (first (filter #(:is_id (get fields %)) stfs))
        id_kw (keyword id)
        title (common/format-title table)]
    (common/page
     title
     
     [:br]
     [:div {:class "ct"} (str (str/capitalize action) " " title)]

     (f/form-to
      {:id (str action "-form")}
      (if (= action "edit")
        [:put (str "/" schema "/" table "/" (id_kw data))]
        [:post (str "/" schema "/" table)])
      (af/anti-forgery-field)
      [:table {:class "form"}
       [:tr
        [:td {:class "lnk" :style "text-align:left;" } [:a {:href "/"} "Home"]]
        [:td {:class "lnk" :style "text-align:right"} [:a {:href "/help.html"} "Help"]]]
       (for [stf stfs
             :let [field (get fields stf)
                   value (db-to-form field ((keyword stf) data))
                   common-attrs
                   (into
                    {:class "fld"}
                    (map
                     (fn [k] [k (get field k)]))
                    ;; attrs common to all controls
                    [:disabled :readonly :required])]]
         [:tr
          (if (or (= (:control field) "textarea")
                  (and (or (= (:control field) "select-option") (= (:control field) "select-result"))
                       (> (:select_size field) 1)))
            [:td {:class "lbl" :style "vertical-align:top"} (form-label stf field)]
            [:td {:class "lbl"} (form-label stf field)])
          
          [:td {:class "fld"} (form-field field stf common-attrs value)]])])

     [:br]

     (when-let [study_people_participants_id (:study.people.participants_id data)]
       [:tr
        [:td {:class "lnk" :style "text-align:left;" }
         [:a {:href (str "/study/participants_people/new?study.participants_people.participants_id=" study_people_participants_id)} "New"]]])

     (when-let [sys_event_classes_event_classes_id (:sys.event_classes.event_classes_id data)]
       [:tr
        [:td {:class "lnk" :style "text-align:left;" }
         [:a {:href (str "/sys/event_classes_fields/new?sys.event_classes_fields.event_classes_id=" sys_event_classes_event_classes_id)} "New"]]])
          
     [:div {:class "ct"}
      (cond
        (= action "edit")
        (list
         (f/submit-button {:form (str action "-form")} "Update")
         "&nbsp;" "&nbsp;"
         (f/form-to
          {:id "delete-form" :style "display: inline-block;"}
          [:delete (str "/" schema "/" table "/" (id_kw data))]
          (af/anti-forgery-field)
          (f/submit-button
           {:onclick "return confirm('Delete this record?');return false;"}
           "Delete"))
         "&nbsp;" "&nbsp;")
        (= action "new")
        (list
         (f/submit-button {:form (str action "-form")} "Insert")
         "&nbsp;" "&nbsp;"))
      [:input {:type "button" :onclick (format "location.href='/%s/%s'" schema table) :value "Cancel"}]]

     [:br])))


(defn new [fields schema table data]
  (let [fields (models/fields-by-schema-table fields schema table)

        fields
        (into
         {}
         (filter
          identity
          (map
           (fn [[k v]]
             (cond (not (:is_settable v))
                   ;; if :is_settable is not true then do not include the field in the new form
                   false
                   ((set ["created_by" "created_at" "updated_by" "updated_at"]) (:field_name v))
                   ;;  drop created_* updated_* fields from the new form
                   false
                   :else [k v]))
           fields)))

        fields
        ;; set disabled to true for fields that are in data (query-params)
        (into {} (map (fn [[k v]] [k (if (get data k) (assoc v :disabled true) v)]) fields))
        
        data
        (into
         {}
         (map
          (fn [[k v]] [(keyword k) (models/ui-to-db-one fields k v)])
          data))]
    
    (form fields schema table "new" data)))


(defn edit [fields schema table data]
  (let [fields (models/fields-by-schema-table fields schema table)]
    (form fields schema table "edit" data)))


(defn show [fields schema table data]
  (let [fields (models/fields-by-schema-table fields schema table)
        fields (into {} (map (fn [[k v]] [k (assoc v :readonly true)]) fields))]
    (form fields schema table "show" data)))

