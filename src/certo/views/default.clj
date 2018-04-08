(ns certo.views.default
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
   [certo.utilities :as u]))


(def ^:const foreign-key-static-field-column-width 12)


(defn format-title [t]
  (str/join " " (map str/capitalize (str/split t #" |_"))))


(defn db-to-ui-datetime [dt]
  "yyyy-MM-ddThh:mm"
  (when dt
    (str (jt/format "YYYY-MM-dd" (jt/local-date dt)) "T" (jt/format "HH:mm" (jt/local-time dt)))))


(defn db-to-ui-timestamp [ts]
  (when ts
    (jt/format "MM/dd/YYYY, hh:mm:ss a" (jt/local-date-time ts))))


(defn db-to-ui-boolean-table [fields field value]
  (cond
    (true? value) (:boolean_true (get fields (name field)))
    (false? value) (:boolean_false (get fields (name field)))
    :else
    (throw (Exception. (format "Invalid boolean value %s" value)))))


(defn db-to-ui-date-table [d]
  (when d
    (jt/format "MM/dd/YYYY" (jt/local-date-time d))))


(defn db-to-ui-select-table [fields field value]
  (some
   (fn [[lbl val]] (when (= val value) lbl))
   (get-in fields [(name field) :options])))


(defn db-to-table-one [fields field value]
  [field
   ;; value transformed to string
   (if (not (nil? value))
     (let [select? (= (get-in fields [(name field) :control]) "select")]
       (condp = (class value)
         java.lang.Boolean (db-to-ui-boolean-table fields field value)
         java.lang.Double value
         java.lang.Integer (if select? (db-to-ui-select-table value) value)         
         java.lang.Long (if select? (db-to-ui-select-table value) value)
         java.lang.String (if select? (db-to-ui-select-table fields field value) value)
         java.sql.Date (db-to-ui-date-table value)
         java.sql.Timestamp (db-to-ui-timestamp value)
         java.util.UUID value
         (throw (Exception. (format "Unknown %s for value %s" (class value) value)))))
     "")])


(defn db-to-table [fields row]
  (into
   {}
   (map  (fn [[field value]] (db-to-table-one fields field value)) row)))


(defn page [title & body]
  (p/html5
   [:head
    (p/include-css "/styles.css")
    [:title title]]
   [:body body]))


(defn table [fields schema table rows]
  (let [fields (models/sort-by-location
                (models/fields-by-schema-table-and-in-table-view fields schema table))
        stfs (map key fields)
        ;; there should be exactly one pk field
        pk (filter #(:is_pk (get fields %)) stfs)        
        pk (case (count (take 2 pk))
             0 (throw (Exception. "No primary key found, but expected exactly one."))             
             1 (first pk)
             (throw (Exception. "Multiple primary keys found, but expected exactly one.")))
        pk_kw (keyword pk)
        title (format-title table)]
    (page
     title
     [:br]
     [:div {:class "ct"} title]
     [:table
      [:tr
       [:td
        {:class "lnk" :style "text-align:left;" } [:a {:href "/"} "Home"]]
       [:td
        {:class "lnk" :style "text-align:right" :colspan (str (dec (count stfs)))} [:a {:href (str "/" schema "/" table "/new")} "New"]]]
      [:tr
       (for [field (map #(get fields %) stfs)]
         [:th (:label field)])]
      (for [row rows
            :let [row (db-to-table fields row)]]
        [:tr
         (for [stf stfs]
           (if (= stf pk)
             [:td [:a {:href (str "/" schema "/" table "/" (pk_kw row) "/edit")} (pk_kw row)]]
             [:td (get row (keyword stf))]))])]
     [:br])))


(defn form-label [name field]
  (f/label name (:label field)))


(defelem date-field
  "Creates a new date input field."
  ([name] (date-field name nil))
  ([name value]
   (#'f/input-field "date" name value)))


(defelem datetime-field
  "Creates a new datetime input field. This control expects the following format 2017-06-01T14:30"
  ([name] (datetime-field name nil))
  ([name value] (#'f/input-field "datetime-local" name (db-to-ui-datetime value))))


(defelem number-field
  "Creates a new number input field.  To restrict the input to
  integers, the caller includes the attribute {:step k} for some
  integer k, and to specify the number of digits after the decimal
  point input for a float, the caller includes the attribute {:step x}
  where x is 0.1, 0.01, etc."
  ([name] (datetime-field name nil))
  ([name value]
   (#'f/input-field "number" name value)))


(defelem timestamp-field
  "Creates a new timestamp input field."
  ([name] (timestamp-field name nil))
  ([name value] (f/text-field name (db-to-ui-timestamp value))))


(defelem boolean-select-field
  "Creates a new boolean-select input field."
  ([name options] (boolean-select-field name options nil))
  ([name options value]
   (f/drop-down
    name
    options
    ;; value
    (cond
      (nil? value) nil
      (true? value) "true"
      (false? value) "false"
      :else
      (throw (Exception. (format "Invalid boolean value %s of class %s is nil? %s" value (class value) (nil? value))))))))


(defn foreign-key-static-field-format [value]
  (if (not (nil? value))
    (condp = (class value)
      java.lang.Boolean value ;; (db-to-ui-boolean-table fields field value)
      java.lang.Double value
      java.lang.Integer value
      java.lang.Long value
      java.lang.String value
      java.sql.Date (db-to-ui-date-table value)
      java.sql.Timestamp (db-to-ui-timestamp value)
      java.util.UUID value
      (throw (Exception. (format "Unknown %s for value %s" (class value) value))))
    ""))


(defelem foreign-key-static-field
  "Creates a new foreign-key-static input field."
  ([name foreign-keys] (foreign-key-static-field name foreign-keys nil))
  ([name foreign-keys value]
   ;;  Cheap way to get a table to select or display data: use a
   ;;  select control with a fixed width font and pad fields with
   ;;  non-breaking spaces so they line up in columns.
   (f/drop-down
    name
    (map
     (fn [foreign-key]
       (vector
        (map
         (fn [[k v]] (u/pads (str (foreign-key-static-field-format v)) foreign-key-static-field-column-width "&nbsp;" true))
         (:label-fields foreign-key))
        (:value foreign-key)))
     foreign-keys)
    value)))


(defn form-field [name field value]
  (let [control (:control field)
        attrs
        (into
         {:class "fld"}
         (filter
          identity
          (map
           (fn [[in out]] (if (get field in) [out (get field in)] false))
           {:text_max_length :maxlength 
            :date_min :min
            :date_max :max
            :foreign_key_size :size
            :integer_step :step
            :integer_min :min
            :integer_max :max
            :float_step :step
            :float_min :min
            :float_max :max
            :select_multiple :multiple
            :select_size :size
            :disabled :disabled
            :readonly :readonly
            :required :required})))]
    (case control
      "date" (date-field attrs name value)
      "datetime" (datetime-field attrs name value)
      ;; "float" (number-field (assoc attrs :step 0.0000000001) name value)
      "float" (number-field attrs name value)
      "foreign-key-static"
       (foreign-key-static-field (assoc attrs :class "fks") name (:foreign-keys field) value)
      ;; "integer" (number-field (assoc attrs :step 1) name value)
      "integer" (number-field attrs name value)
      "integer-key" (number-field attrs name value)
      "select" (f/drop-down attrs name (:options field) value)
      "text" (f/text-field attrs name value)
      "textarea" (f/text-area attrs name value)
      "timestamp" (timestamp-field attrs name value)
      "boolean-select"
      (let [options (map vector
                         ["" (:boolean_true field) (:boolean_false field)]
                         [nil "true" "false"])]
        (boolean-select-field attrs name options value))
      (throw (Exception. (format "Invalid control: %s" control))))))


(defn form [fields schema table action data]
  (let [fields (models/sort-by-location fields)
        stfs (map key fields)
        ;; there should be exactly one pk field
        pk (first (filter #(:is_pk (get fields %)) stfs))
        pk_kw (keyword pk)
        title (format-title table)]
    (page
     title
     
     [:br]
     [:div {:class "ct"} (str (str/capitalize action) " " title)]

     (f/form-to
      {:id (str action "-form")}
      (if (= action "edit")
        [:put (str "/" schema "/" table "/" (pk_kw data))]
        [:post (str "/" schema "/" table)])
      (af/anti-forgery-field)
      [:table {:class "form"}
       [:tr
        [:td {:class "lnk" :style "text-align:left;" } [:a {:href "/"} "Home"]]
        [:td {:class "lnk" :style "text-align:right"} [:a {:href "/help.html"} "Help"]]]
       (for [stf stfs
             :let [field (get fields stf)
                   value ((keyword stf) data)]]
         [:tr
          (if (or (= (:control field) "textarea")
                  (= (:control field) "foreign-key-static")
                  ;; TO DO: Remove this - db constraint requires
                  ;; :select_size is not null for select controls.
                  ;; avoids NPE in the case where (:select_size value)
                  ;; is nil, by using 0 as the default value of
                  ;; (:select_size value).
                  (and (= (:control field) "select") (> (or (:select_size field) 0) 1)))
            [:td {:class "lbl" :style "vertical-align:top"} (form-label stf field)]
            [:td {:class "lbl"} (form-label stf field)])
          [:td {:class "fld"} (form-field stf field value)]])])

     [:br]
     
     [:div {:class "ct"}
      (cond
        (= action "edit")
        (list
         (f/submit-button {:form (str action "-form")} "Update")
         "&nbsp;" "&nbsp;"
         (f/form-to
          {:id "delete-form" :style "display: inline-block;"}
          [:delete (str "/" schema "/" table "/" (pk_kw data))]
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


(defn new [fields schema table]
  (let [fields (models/fields-by-schema-table fields schema table)
        ;;  drop created_* updated_* and is_pk
        fields
        (into
         {}
         (filter
          identity
          (map
           (fn [[k v]]
             (cond (:is_pk v)
                   false
                   ((set ["created_by" "created_at" "updated_by" "updated_at"]) (:field_name v))
                   false
                   :else [k v]))
           fields)))]
    (form fields schema table "new" {})))


(defn edit [fields schema table data]
  (let [fields (models/fields-by-schema-table fields schema table)]
    (form fields schema table "edit" data)))


(defn show [fields schema table data]
  (let [fields (models/fields-by-schema-table fields schema table)
        fields (into {} (map (fn [[k v]] [k (assoc v :readonly true)]) fields))]
    (form fields schema table "show" data)))


(defn dashboard [title sts]
  (page
   title
   [:br] [:br]
   [:div {:class "ct"} (format-title title)]
   [:br] [:br]
   (for [[schema tables] sts]
     (list
      [:table {:style "width: 45%"}
       [:tr [:th {:style "text-align:center" :colspan "3"} (str/capitalize schema)]]
       (for [[table cnt] tables]
         [:tr
          [:td {:style "width: 30%"} (format-title table)]
          [:td {:style "width: 35%"} [:a {:href (str "/" schema "/" table)} "All"] (str "&nbsp;&nbsp;(" cnt ")")]
          [:td {:style "width: 35%"} [:a {:href (str "/" schema "/" table "/new")} "New"]]])]
      [:br] [:br]))))


(defn message [title msg]
  (page
   title
   [:br] [:br]
   [:table {:style "width: 50%"}
    [:tr [:th {:style "text-align:center"} title]]
    [:tr
     [:td
      (for [line (str/split-lines msg)]
        (str line "<br>"))]]]))


(defn not-found-body [req]
  (let [title "Error: Page not found"]
    (page
     title
     [:br]
     [:table {:style "width: 50%"}
      [:tr [:th {:colspan "2" :style "text-align:center"} title]]
      (for [[k v] req]
        [:tr
         [:td k]
         [:td (str v)]])])))


(defn not-found [req]
  {:status 404
   :body (not-found-body req)
   :headers {"Content-Type" "text/html"}})

