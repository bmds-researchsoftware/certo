(ns certo.views.table
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
   [certo.views.form :as form]
   [certo.views.common :as common]))


(defn search-link
  ([field value] (search-link field value value))
  ([field value label]
   (let [idf (and (:is_id field) (:fields_id field))
         ukf (and (:is_uk field) (:search_fields_id field))
         fkf (and (:is_fk field) (:search_fields_id field))
         sf (:search_fields_id field)
         ;; TO DO: op (max-privilege-write-op field)
         op "edit"]
     (cond idf
           (let [[schema_name table_name field_name] (str/split idf #"[.]")]
             [:a {:href (str "/" schema_name "/" table_name "/" value "/edit")}
              value])
           (or ukf fkf)
           (let [[schema_name table_name field_name] (str/split (or ukf fkf) #"[.]")]
             [:a {:href (str "/" schema_name "/" table_name "?" (models/stf schema_name table_name field_name) "=" value "&op=" op)}
              label])
           sf
           (let [[schema_name table_name field_name] (str/split sf #"[.]")]
             [:a {:href (str "/" schema_name "/" table_name "?" (models/stf schema_name table_name field_name) "=" value)}
              label])
           :else
           value))))


;; ----- start: db-to-table -----
;;   type   |    control     
;; ---------+----------------
;;  boolean | select-boolean
;;  date    | date
;;  float8  | float
;;  int8    | integer
;;  int8    | select-option
;;  int8    | select-result
;;  serial8 | integer
;;  text    | select-option
;;  text    | select-result
;;  text    | text
;;  text    | textarea


(defmulti db-to-table (fn [{:keys [:type :control]} value] [(keyword type) (keyword control)]))


(defmethod db-to-table [:boolean :select-boolean] [field value]
  (cond
    (nil? value) nil
    (true? value) (:boolean_true field)
    (false? value) (:boolean_false field)
    :else
    (throw (Exception. (format "Invalid boolean value %s of class %s is nil? %s" value (class value) (nil? value))))))


(defmethod db-to-table [:date :date] [field value]
  (when value
    (search-link field value (jt/format "MM/dd/yyyy" (jt/local-date value)))))


(defmethod db-to-table [:timestamptz :datetime] [field value]
  (when value
    (jt/format "MM/dd/yyyy, HH:mm:ss" (jt/local-date-time value))))


;; (defmethod db-to-table [:time :date] [field value]
;;   (when value
;;     (jt/format "MM/dd/yyyy" (jt/local-date-time value))))


(defmethod db-to-table [:float8 :float] [field value]
  value)


(defmethod db-to-table [:int8 :integer] [field value]
  (search-link field value))


(defmethod db-to-table [:int8 :select-option] [field value]
  (some
   (fn [[lbl val]] (when (= val value) lbl))
   (:options field)))


(defmethod db-to-table [:int8 :select-result] [field value]
    (search-link field value))


(defmethod db-to-table [:serial8 :integer] [field value]
  (search-link field value))


(defmethod db-to-table [:text :select-option] [field value]
  (some
   (fn [[lbl val]] (when (= val value) lbl))
   (:options field)))


(defmethod db-to-table [:text :select-result] [field value]
  (search-link field value))


(defmethod db-to-table [:text :text] [field value]
  (search-link field value))


(defmethod db-to-table [:text :textarea] [field value]
  value)


(defmethod db-to-table :default [field value]
  (throw (Exception. (format "db-to-table::invalid type: %s and control: %s for %s" (:type field) (:control field) (:field_id field)))))
;; ----- end: db-to-table -----


(defn table [tables fields schema table rows data]
  (let [fields (models/sort-by-location ;;fields
                ;; (models/fields-in-table-view fields)
                (models/fields-by-schema-table-and-in-table-view fields schema table))
        stfs (map key fields)
        title (common/format-title table)]
    
    (common/page
     title
     [:br]
     [:div {:class "ct"} title]
     [:table
      [:tr
       [:td
        {:class "lnk" :style "text-align:left;" } [:a {:href "/"} "Home"]]
       ;; Only provide a New link for tables, i.e. not for views.
       (when (not (:is_view (get tables (models/st schema table))))
         [:td
          {:class "lnk" :style "text-align:right" :colspan (str (dec (count stfs)))} [:a {:href (str "/" schema "/" table "/new")} "New"]])]
      
      [:tr
       (for [field (map #(get fields %) stfs)]
         [:th (:label field)])]

      ;; TO DO: Search does not work on views, so exclude it for now.
      ;; In models/default.clj need to update sql-identifier so that
      ;; it is called "everywhere" and so that it double quotes column
      ;; names for views.
      (when (not (:is_view (get tables (models/st schema table))))
        [:tr
         {:class "sc"}
         (f/form-to
          {:id "search-form"}
          [:get (str "/" schema "/" table)]
          ;; HTTTP GET, so not using anti-forgery field
          ;; (af/anti-forgery-field)
          (for [stf stfs
                :let [field (get fields stf)
                      value (form/db-to-form field (models/ui-to-db-one fields stf (get data stf "")))
                      ;; common-attrs {:disabled false :readonly false :required false}
                      common-attrs {:class "fld" :disabled false :readonly false :required false}]]
              [:td {:class "sc" :style "vertical-align:top"} (form/form-field field stf common-attrs value)]))])

      (when (not (:is_view (get tables (models/st schema table))))
        [:tr
         {:class "sb"}
         [:td {:colspan (count stfs)}
          [:div
           {:class "sb"}
           (let [sep (str/join (repeat 4 " &nbsp; "))]
             (list
              (str/join sep ["Order By"  "And/Or" "Fuzzy"])
              sep
              (f/submit-button {:form "search-form"} "Search")))]]])

      ;; TO DO: If rows is a reducible-query this will work well
      
      (for [row rows]
        [:tr
         (for [stf stfs]
           [:td
            (db-to-table
             (get fields stf)
             (get row (keyword stf)))])])]
     [:br])))

