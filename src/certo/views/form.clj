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


(defmulti form-field (fn [{:keys [:control]} name attrs value fields] [(keyword control)]))


(defmethod form-field [:date] [field name attrs value _]
  (cf/date-field (update-attrs (assoc attrs :style (format "width: %dem;" (count "01/01/0001"))) field {:date_min :min :date_max :max}) name value))


(defmethod form-field [:datetime] [field name attrs value _]
  (cf/datetime-field (assoc attrs :style (format "width: %dem;" (count "01/01/0001, 01:01:01"))) name value))


(defmethod form-field [:float] [field name attrs value _]
  ;; (number-field (assoc attrs :step 0.0000000001) name value)
  (cf/number-field (update-attrs (assoc attrs :style (format "width: %dem;" (:size field))) field {:float_step :step :float_min :min :float_max :max}) name value))


(defmethod form-field [:integer] [field name attrs value _]
  ;; (number-field (assoc attrs :step 1) name value)
  ;; Note: 6em will allow a 6 digit integer to be input.
  ;; TO DO: This should be computed from the integer_max and integer_min.
  (cf/number-field (update-attrs (assoc attrs :style (format "width: %dem;" (:size field))) field {:integer_step :step :integer_min :min :integer_max :max}) name value))


(defmethod form-field [:select-boolean] [field name attrs value _]
  (let [options (map vector
                     ["" (:boolean_true field) (:boolean_false field)]
                     [nil "true" "false"])]
    (cf/select-boolean-field attrs name options value)))


(defmethod form-field [:select-option] [field name attrs value _]
  (f/drop-down (update-attrs attrs field {:select_multiple :multiple :select_size :size}) name (conj (:options field) ["" nil]) value))


(defmethod form-field [:select-result] [field name attrs value fields]
  ;; Cheap way to get a table to select or display data: use a select
  ;; control with a fixed width font and pad fields with non-breaking
  ;; spaces so they line up in columns.
  ;;  (pprint/pprint (keys fields))
  (cf/select-result-field
   (assoc (update-attrs attrs field {:select_size :size}) :class "sr")
   name
   (as-> (into [] (:options field)) options
     (map
      (fn [{value :value :as all}]
        (vector
         (if value
           (map
            ;; TO DO: Should not be calling db-to-label.  Should call
            ;; db-to-table (since it depends on both type and control)
            ;; with a new argument that indicates should not return a
            ;; link, just a value.
            (fn [[k v]]
              (when (nil? (get fields (clojure.core/name k)))
                (throw (Exception. (format "form-field:: %s not found in fields" (clojure.core/name k)))))
              (u/pads
               (str (cf/db-to-label v))
               (or (get-in fields [(clojure.core/name k) :size]) 25)
               "&nbsp;" true))
            (dissoc all :value))
           "")
         value))
      options)
     (conj options ["" nil]))
   value))


(defmethod form-field [:text] [field name attrs value _]
  (f/text-field (update-attrs attrs field {:text_max_length :maxlength :size :size}) name value))


(defmethod form-field [:textarea] [field name attrs value _]
  (f/text-area (update-attrs attrs field {:text_max_length :maxlength :textarea_cols :cols :textarea_rows :rows}) name value))


(defmethod form-field [:timestamp] [field name attrs value _]
  (cf/timestamp-field (update-attrs attrs field {:size :size}) name value))


(defmethod form-field :default [field name attrs value _]
  (throw (Exception. (format "form-field::invalid control: %s" (:control field)))))
;; ----- end: form-field -----


(defn form-label [name field]
  (if (:required field)
    (f/label name (str (:label field) "*"))
    (f/label name (:label field))))


(defn form [all-fields schema table action data]
  ;; exclude fields that are the columns in a select-result so they
  ;; aren't displayed at the top level of the form
  (let [fields (models/fields-by-schema-table all-fields schema table)
        fields (models/sort-by-location fields)
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
          
          [:td {:class "fld"} (form-field field stf common-attrs value all-fields)]])])

     [:br]

     (when-let [study_people_participants_id (:study.people.participants_id data)]
       [:tr
        [:td {:class "lnk" :style "text-align:left;" }
         [:a {:href (str "/study/participants_people/new?study.participants_people.participants_id=" study_people_participants_id)} "New"]]])

     (when-let [sys_event_classes_event_classes_id (:sys.event_classes.event_classes_id data)]
       [:tr
        [:td {:class "lnk" :style "text-align:left;" }
         [:a {:href (str "/sys/event_class_fields/new?sys.event_class_fields.event_classes_id=" sys_event_classes_event_classes_id)} "New"]]])
          
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
  (form fields schema table "edit" data))


(defn show [fields schema table data]
  (let [fields (into {} (map (fn [[k v]] [k (assoc v :readonly true)]) fields))]
    (form fields schema table "show" data)))

