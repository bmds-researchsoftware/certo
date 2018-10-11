(ns certo.views.login
  (:require
   [clojure.string :as str]
   [hiccup.core :as h]
   [hiccup.form :as f]
   [ring.util.anti-forgery :as af]
   [certo.views.common :as common]))


(defn login [title]
  (common/page
   title
   [:br] [:br]

   [:div {:class "ct"} (common/format-title title)]
   
   (f/form-to
    {:id "login-form"}
    [:post "login"]
    (af/anti-forgery-field)
    [:table {:class "login"}
     
     [:tr
      [:td {:class "lnk" :style "text-align:left;" } [:a {:href "/"} "Home"]]
      [:td {:class "lnk" :style "text-align:right"} [:a {:href "/help.html"} "Help"]]]

     [:tr [:th {:style "text-align:center" :colspan "2"} (str/capitalize "Login")]]
     
     [:tr
      [:td {:class "lbl"} (f/label name "Username:")]
      [:td {:class "fld"}
       (f/text-field {:required true :size 32 :class "fld"} "username")]]

     [:tr
      [:td {:class "lbl"} (f/label name "Password:")]
      [:td {:class "fld"}
       (f/password-field {:required true :size 32 :class "fld"} "password")]]

     [:tr [:td {:style "text-align:center" :colspan "2"} (f/submit-button {:form "login-form"} "Login")]]])))

