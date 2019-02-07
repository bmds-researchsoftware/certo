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


(defn compare-order-by-fields [[score1 label1 field_name1] [score2 label2 field_name2]]
  (cond
    (< score1 score2) 1
    (> score1 score2) -1
    :else (compare label1 label2)))


(defn offset [data]
  ;; TO DO: NEED TO ALWAYS REQUIRE LIMIT AND OFFSET - THIS MIGHT NEED TO BE DONE IN models/default.clj
  (let [offset (u/parse-integer (get data "offset" "0") "Offset" (fn [x] (>= x 0)))
        limit (u/parse-integer (get data "limit" "25") "Limit" pos?)]
    {:previous (max 0 (- offset limit)) :next (+ offset limit)}))


(defn row-range [data number-rows]
  ;; TO DO: NEED TO ALWAYS REQUIRE LIMIT AND OFFSET - THIS MIGHT NEED TO BE DONE IN models/default.clj
  (let [offset (u/parse-integer (get data "offset" "0") "Offset" (fn [x] (>= x 0)))
        limit (u/parse-integer (get data "limit" "25") "Limit" pos?)]
    {:lower (inc offset) :upper (+ number-rows offset)}))


(defn previous-next-button [base-url data direction]
  [:button
   {:type "button" :onclick (format "window.location.href='%s?%s';" base-url (ring.util.codec/form-encode (assoc data "offset" (direction (offset data)))))}
   (cond (= direction :previous) "<"
         (= direction :next) ">"
         :else (throw (Exception. (format "Invalid direction: %s" direction))))])


(defn table [table-map fields schema table rows base-url data]
  (if (zero? (count rows))
    
    (common/message "Message" "None found")

    (let [fields (models/sort-by-location
                  (models/fields-by-schema-table-and-in-table-view fields schema table))
          stfs (map key fields)
          title (common/format-title table)
          order-by-fields
          (map
           (fn [[score label field_name]] (vector label field_name))
           (sort-by
            identity
            compare-order-by-fields
            (map
             ;; the score order is: id_id > is_uk > is_fk
             (fn [field] (vector (+ (if (:is_id field) 100 0) (if (:is_uk field) 10 0) (if (:is_fk field) 1 0)) (:label field) (:field_name field)))
             (vals fields))))]

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
                      value (form/db-to-form field (models/ui-to-db-one fields stf (get data stf "")))
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
               "Match" small-sep (f/drop-down "operator" [["some values" "or"] ["all values" "and"]] (get data "operator" "or"))
               sep
               "Match" small-sep (f/drop-down "comparator" [["beginning of value" "beginning"] ["value approximately" "approximate"] ["value exactly" "exact"]] (get data "comparator" "beginning"))
               sep
               "Sort by" small-sep (f/drop-down "order-by" order-by-fields (get data "order-by" (second (first order-by-fields))))
               sep
               "Sort in" small-sep (f/drop-down "direction" [["increasing order" "asc"] ["decreasing order" "desc"]] (get data "direction" "asc"))
               sep
               ;; TO DO: Get defaults preferably from config.
               "Display" small-sep (f/drop-down "limit" [["25 rows" "25"] ["50 rows" "50"] ["100 rows" "100"] ["250 rows" "250"] ["500 rows" "500"]] (get data "limit" "25"))
               sep
               [:button {:form "search-form" :name "offset" :value "0"} "Search"]]
              [:div {:class "sbright"}
               (format "%d-%d of %,d" (:lower (row-range data (count rows))) (:upper (row-range data (count rows))) 1000000) ;; TO DO: Fix this 1000000
               small-sep
               (previous-next-button base-url data :previous)
               small-sep
               (previous-next-button base-url data :next)]))]]

         ;; TO DO: If rows is a reducible-query this will work well

         (for [row rows]
           [:tr
            (for [stf stfs]
              [:td {:style "white-space: pre; vertical-align:top;"}
               (db-to-table
                (get fields stf)
                (get row (keyword stf)))])])])

       [:br]))))

