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


(defn dashboard [title user sts]
  (common/page
   title
   [:br] [:br]
   [:div {:class "ct"} (common/format-title title)]
   [:br]
   [:table {:style "width: 45%"}
    [:tr
     [:th {:style "width: 35%; border: 0px; font-size: 85%; text-align:left"} (:display_name user)]
     [:th {:style "width: 35%; border: 0px; font-size: 85%; text-align:center"} (:usergroup_label user)]
     ;; [:th {:style "width: 35%; border: 0px; font-size: 85%; text-align:right"} (jt/format "MM/dd/yyyy"(u/date-now))]
     [:th {:style "width: 30%; border: 0px; font-size: 85%; text-align:right"} "Version: 0.1.0"]]]
   [:br]
   (for [[schema tables] sts]
     (list
      [:table {:style "width: 45%"}
       [:tr [:th {:style "text-align:center" :colspan "3"} (str/capitalize schema)]]
       (for [{:keys [table is_view count]} tables]
         [:tr
          [:td {:style "width: 30%"} (common/format-title table)]
          [:td {:style "width: 35%"} [:a {:href (str "/" schema "/" table)} "All"] (str "&nbsp;&nbsp;(" count ")")]
          [:td {:style "width: 35%"}
           (if (not is_view)
             [:a {:href (str "/" schema "/" table "/new")} "New"]
             [:br])]])]
      [:br] [:br]))))

