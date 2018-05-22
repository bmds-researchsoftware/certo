(ns certo.views.common
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


(defn page [title & body]
  (p/html5
   [:head
    (p/include-css "/styles.css")
    [:title title]]
   [:body body]))


(defn format-title [t]
  (str/join " " (map str/capitalize (str/split t #" |_"))))


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

