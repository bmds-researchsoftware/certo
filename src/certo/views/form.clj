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


(defn filter-select-result-function-name [fields_id]
  (str (str/replace fields_id "." "_") "_fsr_fn"))


(defn filter-select-result-search-field-name [fields_id]
  (str (str/replace fields_id "." "_") "_fsr_sfn"))


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
  (if (:readonly attrs)
    (f/text-field {:readonly true :size (count value) :class "fld"} name value)
    (f/drop-down (update-attrs attrs field {:select_multiple :multiple :select_size :size}) name (conj (:options field) ["" nil]) value)))


(defmethod form-field [:select-result] [field name attrs value fields]
  ;; Cheap way to get a table to select or display data: use a select
  ;; control with a fixed width font and pad fields with non-breaking
  ;; spaces so they line up in columns.
  (if (:readonly attrs)
    (f/text-field {:readonly true :size (count (str value)) :class "fld"} name value)
    [:div
     [:script
      (format "var %s = filterSelectResult(\"%s\", \"%s\");"
              (filter-select-result-function-name (:fields_id field))
              (filter-select-result-search-field-name (:fields_id field))
              (:fields_id field))]
     (let [fsrfn (format "return %s(event);" (filter-select-result-function-name name))]
       (f/text-field
        {:class "fld"
         ;; set name attribute to nil so not included in form submission
         :name nil
         :id (filter-select-result-search-field-name (:fields_id field))
         :placeholder "Enter search text" :oninput fsrfn}
        (filter-select-result-search-field-name (:fields_id field))))
     [:div {:class "fsrsf"}]
     (f/drop-down
      (assoc (update-attrs attrs field {:select_size :size}) :class "sr")
      name
      (as-> (into [] (map (:transducer (:options field))) (:reducible-query (:options field))) options
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
                 (when (nil? (get fields k))
                   (throw (Exception. (format "form-field:: %s not found in fields" k))))
                 ;; TO DO: All fields must have a size - enforce it with a database constraint
                 ;; (when (nil? (get-in fields [k :size]))
                 ;;   (throw (Exception. (format "form-field:: size for %s not found" k))))
                 (u/pads
                  (str (cf/db-to-label v))
                  (or
                   (get-in fields [k :size])
                   (count (str (cf/db-to-label v))))
                  ;; (get-in fields [k :size])
                  "&nbsp;" true))
               (dissoc all :value))
              "")
            value))
         options)
        (conj
         options
         [""
          nil]))
      value)]))


(defmethod form-field [:hidden] [field name attrs value _]
  (f/hidden-field name value))


(defmethod form-field [:password] [field name attrs value _]
  (f/password-field (assoc (update-attrs attrs field {:text_max_length :maxlength :size :size}) :minlength 16) name value))


(defmethod form-field [:text] [field name attrs value _]
  (f/text-field (update-attrs attrs field {:text_max_length :maxlength :size :size}) name value))


(defmethod form-field [:textarea] [field name attrs value _]
  (f/text-area (update-attrs attrs field {:text_max_length :maxlength :textarea_cols :cols :textarea_rows :rows}) name value))


(defmethod form-field [:time] [field name attrs value _]
  (cf/time-field (update-attrs (assoc attrs :style (format "width: %dem;" (count "00:00 AM"))) field {:time_min :min :time_max :max}) name value))


(defmethod form-field [:timestamp] [field name attrs value _]
  (cf/timestamp-field (update-attrs attrs field {:size :size}) name value))


(defmethod form-field :default [field name attrs value _]
  (throw (Exception. (format "form-field::invalid control: %s" (:control field)))))
;; ----- end: form-field -----


(defn form-label [name field]
  (if (:required field)
    (f/label name (str (:label field) "*"))
    (f/label name (:label field))))


(defn form [all-fields schema table action redirect-to data]
  ;; exclude fields that are the columns in a select-result so they
  ;; aren't displayed at the top level of the form
  (let [fields (models/fields-by-schema-table all-fields schema table)
        fields (models/sort-by-location fields)
        fields (if (and (= schema "event") (= ((keyword (models/stf schema table "event_queue_id")) data) 0))
                 (dissoc fields (models/stf schema table "event_queue_id"))
                 fields)
        data (if (and (= schema "event") (= ((keyword (models/stf schema table "event_queue_id")) data) 0))
                 (dissoc data (keyword (models/stf schema table "event_queue_id")))
                 data)
        fields (if (and (= schema "sys") (= table "events") (false? (:sys.events.is_time_required data)))
                 (dissoc fields "sys.events.event_time")
                 fields)
        data (if (and (= schema "sys") (= table "events") (false? (:sys.events.is_time_required data)))
                 (dissoc data :sys.events.event_time)
                 data)
        stfs (map key fields)
        ;; there should be exactly one id field
        id (first (filter #(:is_id (get fields %)) stfs))
        id_kw (keyword id)
        title (common/format-title table)]
    (common/page
     title
     
     [:br]
     [:div {:class "ct"}
      (if (= schema "event")
        title
        (str (str/capitalize action) " " title))]

     (f/form-to
      {:id (str action "-form")}
      (if (= action "edit")
        [:put (str "/" schema "/" table "/" (id_kw data))]
        [:post (str "/" schema "/" table)])
      (f/hidden-field :redirect-to redirect-to)
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
          
          [:td {:class "fld"}
           (form-field field stf common-attrs value all-fields)]])])

     [:br]
          
     [:div {:class "ct"}

      (cond
        (= action "edit")
        (list
         (f/submit-button {:form (str action "-form")} "Update")
         "&nbsp;" "&nbsp;"
         (f/form-to
          {:id "delete-form" :style "display: inline-block;"}
          [:delete (str "/" schema "/" table "/" (id_kw data))]
          (f/hidden-field :redirect-to redirect-to)
          (af/anti-forgery-field)
          (f/submit-button
           {:onclick "return confirm('Delete this record?');return false;"}
           "Delete"))
         "&nbsp;" "&nbsp;")

        (= action "new")
        (list
         (f/submit-button {:form (str action "-form")} "Insert")
         "&nbsp;" "&nbsp;"))

      (when-let [sys_event_classes_event_classes_id (:sys.event_classes.event_classes_id data)]
        (list
         [:input
          {:type "button"
           :onclick (format "location.href='%s'" (str "/sys/event_class_fields/new?sys.event_class_fields.event_classes_id=" sys_event_classes_event_classes_id))
           :value "Add Event Class Fields"}]
         "&nbsp;" "&nbsp;"))

      (when-let [study_people_participants_id (:study.people.participants_id data)]
        (list
         [:input
          {:type "button"
           :onclick (format "location.href='%s'" (str "/study/participants_people/new?study.participants_people.participants_id=" study_people_participants_id))
           :value "Add People"}]
         "&nbsp;" "&nbsp;"))

      [:input {:type "button"
               :onclick (if (= schema "event")
                          "history.go(-1); return false;"
                          (format "location.href='/%s/%s'" schema table))
               :value "Cancel"}]]

     [:br])))


(defn new [fields schema table redirect-to data]
  (let [fields
        (into
         {}
         (filter
          identity
          (map
           (fn [[k v]]
             (cond (or (not (:is_settable v)) (:is_legacy v))
                   ;; if :is_settable=false or :is_legacy=true then do not include the field in the new form
                   false
                   ((set ["created_by" "created_at" "updated_by" "updated_at"]) (:field_name v))
                   ;;  drop created_* updated_* fields from the new form
                   false
                   :else [k v]))
           fields)))
        fields
        ;; set disabled to true for fields that are in data (query-params)
        (into {} (map (fn [[k v]] [k (if (get data k) (assoc v :readonly true) v)]) fields))
        data
        (into
         {}
         (map
          (fn [[k v]] [(keyword k) (models/ui-to-db-one fields k v)])
          data))]
    (form fields schema table "new" redirect-to data)))


(defn edit [fields schema table redirect-to data]
  (form fields schema table "edit" redirect-to data))


(defn show [fields schema table redirect-to data]
  (let [fields (into {} (map (fn [[k v]] [k (assoc v :readonly true)]) fields))]
    (form fields schema table "show" redirect-to data)))

