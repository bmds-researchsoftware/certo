(ns certo.views.core
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
   [certo.views.form :as frm]
   [certo.views.table :as t]
   [certo.views.dashboard :as d]
   [certo.views.common :as common]))


(defn dashboard [title user sts] (d/dashboard title user sts))
(defn table [tables fields schema table rows data] (t/table tables fields schema table rows data))
(defn new [fields schema table data] (frm/new fields schema table data))
(defn edit [fields schema table data] (frm/edit fields schema table data))
(defn show [fields schema table data] (frm/show fields schema table data))


(defn message [title msg]
  (common/page
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
    (common/page
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

