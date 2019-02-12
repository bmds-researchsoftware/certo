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
     (cond (= (:fields_id field) "app.event_queue.event_queue_id")
           (let [query-string
                 (str/join
                  "&"
                  (map
                   (fn [[k v]] (str "event." (:event-classes-id value) "." (name k) "=" v))
                   (into {:event_queue_id (:event-queue-id value)} (:event-class-argument-dimensions value))))]
             [:a {:style "font-weight: bold;" :href (str "/event/" (:event-classes-id value) "/new" (if (not (str/blank? query-string)) (str "?" query-string) ""))}
              (if (= (:event-queue-id value) 0)
                "*"
                (:event-queue-id value))])
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


;; TO DO: Check if this is displaying UTC in the table view.
(defmethod db-to-table [:timestamptz :datetime] [field value]
  (when value
    (jt/format "MM/dd/yyyy, hh:mm a" (jt/local-date-time value))))


;; TO DO: This is displaying UTC in the table view - fix it.
(defmethod db-to-table [:timestamptz :timestamp] [field value]
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


(defn previous-next-offset [offset limit cnt-all]
  {:previous (max 0 (- offset limit)) :next (if (<= (+ offset limit) cnt-all) (+ offset limit) offset)})


(defn row-range [offset cnt]
  {:lower (inc offset) :upper (+ cnt offset)})


(defn previous-next-button [base-url params offset limit cnt-all direction]
  [:button
   {:type "button"
    :onclick (format "window.location.href='%s?%s';" base-url (ring.util.codec/form-encode (assoc params "offset" (direction (previous-next-offset offset limit cnt-all)))))}
   (cond (= direction :previous) "<"
         (= direction :next) ">"
         :else (throw (Exception. (format "Invalid direction: %s" direction))))])


(defn table [table-map fields schema table rows cnt cnt-all base-url params]
  (if (zero? cnt)
    
    (common/message "Message" "None found")

    (let [fields (models/sort-by-location
                  (models/fields-by-schema-table-and-in-table-view fields schema table))
          order-by-fields
          (map
           (fn [{:keys [label fields_id]}] (vector label fields_id))
           (vals fields))
          offset-int (Integer/parseInt (get params "offset"))
          limit-int (Integer/parseInt (get params "limit"))
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
           {:class "lnk" :style "text-align:left;" :colspan "1"}
           [:a {:href "/"} "Home"]]
          ;; Only provide a New link for tables, i.e. not for views or result_views.
          (when (not (or (:is_view table-map) (:is_result_view table-map)))
            [:td
             {:class "lnk"
              :style "text-align:center" :colspan (- (count stfs) 2)}
             [:a {:href (str "/" schema "/" table "/new")} "New"]])
          (if (not (or (:is_view table-map) (:is_result_view table-map)))
            [:td
             {:class "lnk" :style "text-align:right" :colspan "1"}
             [:a {:href "/help.html"} "Help"]]
            [:td
             {:class "lnk" :style "text-align:right" :colspan (- (count stfs) 1)}
             [:a {:href "/help.html"} "Help"]])]

         [:tr
          (for [field (map #(get fields %) stfs)]
            [:th (:label field)])]

         [:tr
          {:class "sc"}
          (for [stf stfs
                :let [field (get fields stf)
                      value (form/db-to-form field (models/ui-to-db-one fields stf (get params stf "")))
                      common-attrs {:class "fld" :disabled false :readonly false :required false}]]
            [:td {:class "sc" :style "vertical-align:top"}
             (cond (and (= (:control field) "select-result") (:select_result_to_text field))
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
                           {:control "text" :size (or (:size field) 50) :text_max_length 1024}
                           :else (throw (Exception. (format "Invalid type: %s for control: select-result" (:type field))))))
                    (form/form-field stf common-attrs value fields))
                   ;; remove datetime control from search
                   (= (:control field) "datetime")
                   "<br>"
                   :else
                   (form/form-field field stf common-attrs value fields))])]

         [:tr
          [:td {:colspan (count stfs)}
           (let [sep (str/join (repeat 6 "&nbsp;"))
                 small-sep "&nbsp;"]
             (list
              [:div {:class "sbleft"}
               "Match" small-sep (f/drop-down "operator" [["some values" "or"] ["all values" "and"]] (get params "operator" "or"))
               sep
               "Match" small-sep (f/drop-down "comparator" [["beginning of value" "beginning"] ["value approximately" "approximate"] ["value exactly" "exact"]] (get params "comparator" "beginning"))
               sep
               "Sort by" small-sep (f/drop-down "order-by" order-by-fields (get params "order-by"))
               sep
               "Sort in" small-sep (f/drop-down "direction" [["increasing order" "asc"] ["decreasing order" "desc"]] (get params "direction"))
               sep
               ;; TO DO: Get defaults preferably from config.
               "Display" small-sep (f/drop-down "limit" [["25 rows" "25"] ["50 rows" "50"] ["100 rows" "100"] ["250 rows" "250"] ["500 rows" "500"]] (get params "limit"))
               sep
               [:button {:form "search-form" :name "offset" :value "0"} "Search"]]
              [:div {:class "sbright"}
               (format "%d-%d of %,d" (:lower (row-range offset-int cnt)) (:upper (row-range offset-int cnt)) cnt-all)
               small-sep
               (previous-next-button base-url params offset-int limit-int cnt-all :previous)
               small-sep
               (previous-next-button base-url params offset-int limit-int cnt-all :next)]))]]

         ;; TO DO: If rows is a reducible-query this will work well

         (for [row rows]
           [:tr
            (for [stf stfs]
              [:td {:style "white-space: pre; vertical-align:top;"}
               (db-to-table
                (get fields stf)
                (get row (keyword stf)))])])])

       [:br]))))

