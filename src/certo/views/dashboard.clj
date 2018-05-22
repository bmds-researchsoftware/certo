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



(defn dashboard [title sts]
  (common/page
   title
   [:br] [:br]
   [:div {:class "ct"} (common/format-title title)]
   [:br] [:br]
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


