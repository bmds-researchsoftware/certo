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


(defn db-to-table-time [value]
 (jt/format "hh:mm a" (jt/local-time value)))

(defn search-link
  ([field value] (search-link field value value))
  ([field value label]
   (let [idf (and (:is_id field) (:fields_id field))
         ukf (and (:is_uk field) (:search_fields_id field))
         fkf (and (:is_fk field) (:search_fields_id field))
         sf (:search_fields_id field)
         ;; TO DO: op (max-privilege-write-op field)
         op "edit"]
     (cond (= (:fields_id  field) "app.event_queue.event_queue_link")
           (let [query-string
                 (str/join
                  "&"
                  (map
                   (fn [[k v]] (str "event." (:event-classes-id value) "." (name k) "=" v))
                   (:event-class-argument-dimensions value)))]
             [:a {:href (str "/event/" (:event-classes-id value) "/new" (if (not (str/blank? query-string)) (str "?" query-string) ""))}
              (:event-queue-id value)])
           idf
           (let [[schema_name table_name field_name] (str/split idf #"[.]")]
             [:a {:href (str "/" schema_name "/" table_name "/" value "/edit")}
              value])
           (or ukf fkf)
           (let [[schema_name table_name field_name] (str/split (or ukf fkf) #"[.]")]
             [:a {:href (str "/" schema_name "/" table_name "?"
                             (models/stf schema_name table_name field_name) "=" value "&op=" op "&operator=and&comparator=exact")}
              label])
           sf
           (let [[schema_name table_name field_name] (str/split sf #"[.]")]
             [:a {:href (str "/" schema_name "/" table_name "?"
                             (models/stf schema_name table_name field_name) "=" value "&operator=and&comparator=exact")}
              label])
           :else
           label))))

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
    (jt/format "MM/dd/yyyy, hh:mm a" (jt/local-date-time value))))


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
  (search-link
   field
   (some
    (fn [[lbl val]] (when (= val value) lbl))
    (:options field))))


(defmethod db-to-table [:text :select-result] [field value]
  (search-link field value))


(defmethod db-to-table [:text :text] [field value]
  (search-link field value))


(defmethod db-to-table [:uuid :text] [field value]
  (search-link field value))


(defmethod db-to-table [:text :textarea] [field value]
  value)


(defmethod db-to-table [:time :time] [field value]
  (when value
    (search-link field value (jt/format "hh:mm a" (jt/local-time value)))))


(defmethod db-to-table :default [field value]
  (throw (Exception. (format "db-to-table::invalid type: %s and control: %s for %s" (:type field) (:control field) value))))
;; ----- end: db-to-table -----


(defn table [table-map fields schema table rows data]
  (let [fields (models/sort-by-location
                (models/fields-by-schema-table-and-in-table-view fields schema table))
        stfs (map key fields)
        title (common/format-title table)]
    
    (common/page
     title
     [:br]
     [:div {:class "ct"} title]

     (f/form-to
      {:id "search-form"}
      [:get (str "/" schema "/" table)]
      ;; HTTTP GET, so not using anti-forgery field
      ;; (af/anti-forgery-field)

      [:table

       [:tr
        [:td
         {:class "lnk" :style "text-align:left;" } [:a {:href "/"} "Home"]]
        ;; Only provide a New link for tables, i.e. not for views or result_views.
        (when (not (or (:is_view table-map) (:is_result_view table-map)))
          [:td
           {:class "lnk"
            :style "text-align:right"
            :colspan (str (dec (count stfs)))} [:a {:href (str "/" schema "/" table "/new")} "New"]])]

       [:tr
        (for [field (map #(get fields %) stfs)]
          [:th (:label field)])]

       ;; TO DO: Search does not work on views, so exclude it for now.
       ;; In models/default.clj need to update sql-identifier so that
       ;; it is called "everywhere" and so that it double quotes column
       ;; names for views.
       (when (not (or (:is_view table-map) (:is_result_view table-map)))
         [:tr
          {:class "sc"}
          (for [stf stfs
                :let [field (get fields stf)
                      value (form/db-to-form field (models/ui-to-db-one fields stf (get data stf "")))
                      common-attrs {:class "fld" :disabled false :readonly false :required false}]]
            [:td {:class "sc" :style "vertical-align:top"}
             (if (and (= (:control field) "select-result") (:select_result_to_text field))
               ;; "convert" select-result control to a text control for search
               (->
                (merge
                 (select-keys
                  field
                  [:fields_id :schema_name :table_name :field_name :type :is_function
                   :is_id :is_uk :is_fk :is_settable :label :location :in_table_view])
                 (cond (= (:type field) "int8")
                       {:control "integer"}
                       (= (:type field) "text")
                       ;; TO DO: Remove hard coded text_size and text_max_length
                       {:control "text" :text_size 25 :text_max_length 40}
                       :else (throw (Exception. (format "Invalid type: %s for control: select-result" (:type field))))))
                (form/form-field stf common-attrs value fields))
               (form/form-field field stf common-attrs value fields))])])

       (when (not (or (:is_view table-map) (:is_result_view table-map)))
         [:tr
          {:class "sb"}
          [:td {:colspan (count stfs)}
           [:div
            {:class "sb"}
            (let [sep (str/join (repeat 4 " &nbsp; "))]
              (list
               ;; "Order By:"
               ;; " &nbsp; "
               ;; (f/drop-down "order-by" [["^ Protocol Name" "protocol_name"] ["v Protocol Name" "protocol_name"]])
               ;; sep
               (f/drop-down "operator" [["Or" "or"] ["And" "and"]] (get data "operator" "or"))
               sep
               (f/drop-down "comparator" [["Approximate" "approximate"] ["Exact" "exact"]] (get data "comparator" "approximate"))
               sep
               (f/submit-button {:form "search-form"} "Search")))]]])

       ;; TO DO: If rows is a reducible-query this will work well
           
       (for [row rows]
         [:tr
          (for [stf stfs]
            [:td {:style "white-space: pre; vertical-align:top;"}
             (db-to-table
              (get fields stf)
              (get row (keyword stf)))])])])
     
     [:br])))

