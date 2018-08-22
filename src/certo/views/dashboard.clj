(ns certo.views.dashboard
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


;; Return something like: https://localhost/event/recruitment_coordinator_entered_pregnancy_screenform/new?event.recruitment_coordinator_entered_pregnancy_screenform.participants_id=182821
(defn event-new-query-string [eqh dimensions]
  (str/join
   "&"
   (filter
    identity
    (map
     (fn [dimension]
       (if-let [value (dimension eqh)]
         (str "event." (:event_classes_id eqh) "." (name dimension) "=" value)
         nil))
     dimensions))))


;; eqh = event queue head
(defn event-new-link [eqh dimensions]
  [:a
   {:href (str "/event/" (:event_classes_id eqh) "/new" (let [qs (event-new-query-string eqh dimensions)] (if (str/blank? qs) "") (str "?" qs)))}
   (:event_queue_id eqh)])


(defn dashboard [title user {:keys [:event-queue :sts]}]
  (common/page
   title
   [:br] [:br]
   [:div {:class "ct"} (common/format-title title)]

   [:br]
   [:table {:style "width: 60%"}
    [:tr
     [:th {:style "width: 35%; border: 0px; font-size: 85%; text-align:left"} (:display_name user)]
     [:th {:style "width: 35%; border: 0px; font-size: 85%; text-align:center"} (:usergroup_label user)]
     ;; [:th {:style "width: 35%; border: 0px; font-size: 85%; text-align:right"} (jt/format "MM/dd/yyyy"(u/date-now))]
     [:th {:style "width: 30%; border: 0px; font-size: 85%; text-align:right"} "Version: 0.1.0"]]]

   [:br]
   (for [[schema tables] sts]
     (list
      [:table {:style "width: 60%"}
       [:tr [:th {:style "text-align:center" :colspan "7"} (str/capitalize schema)]]
       [:tr
        [:th {:style "width: 42%; text-align:left"} "Table"]
        [:th {:style "width: 8%; text-align:center"} "Table?"]
        [:th {:style "width: 8%; text-align:center"} "View?"]
        [:th {:style "width: 8%; text-align:center"} "Option Table?"]
        [:th {:style "width: 8%; text-align:center"} "Result View?"]
        [:th {:style "width: 10%; text-align:center"} "New"]
        [:th {:style "width: 16%; text-align:center"} "Count"]]
       (for [{:keys [table is_table is_view is_result_view is_option_table count]} tables]
         [:tr
          [:td {:style "width: 42%; text-align:left"} (common/format-title table)]
          [:td {:style "width: 8%; text-align:center"} (if is_table "&#10004;" "")]
          [:td {:style "width: 8%; text-align:center"} (if is_view "&#10004;" "")]
          [:td {:style "width: 8%; text-align:center"} (if is_option_table "&#10004;" "")]
          [:td {:style "width: 8%; text-align:center"} (if is_result_view "&#10004;" "")]
          [:td {:style "width: 10%; text-align:center"}
           (if (not (or is_view is_result_view))
             [:a {:href (str "/" schema "/" table "/new")} "New"]
             [:br])]
          [:td {:style "width: 16%; text-align:left"} [:a {:href (str "/" schema "/" table)} "All"] (str "&nbsp;&nbsp;(" count ")")]])]
      [:br] [:br]))))

