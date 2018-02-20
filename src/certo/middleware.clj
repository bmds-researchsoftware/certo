(ns certo.middleware
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]   
   [clojure.edn :as edn]
   [compojure.core :refer [GET POST] :as compojure]
   [compojure.route :as route]
   [ring.util.response :refer [redirect]]
   [certo.views.default :as cvd]))

(defn wrap-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "text/plain"}
         :body (format "Error: %s" (.getMessage e))}))))

;; (defn wrap-exception-web
;;   [handler]
;;   (fn [request]
;;     (try
;;       (handler request)
;;       (catch Exception e
;;         (cvd/message "Error" (.getMessage e))))))

(defn wrap-exception-web
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "text/html"}
         :body (cvd/message "Error" (.getMessage e))}))))

(defn wrap-postgres-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        {:status 400
         :headers {"Content-Type" "text/plain"}
         :body (format "Database Error: %s" (.getMessage e))}))))

;; (defn wrap-postgres-exception-web
;;   [handler]
;;   (fn [request]
;;     (try
;;       (handler request)
;;       (catch org.postgresql.util.PSQLException e
;;         (cvd/message "Database Error" (.getMessage e))))))

(defn wrap-postgres-exception-web
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        {:status 400
         :headers {"Content-Type" "text/html"}
         :body (cvd/message "Database Error" (.getMessage e))}))))

(defn customize-site-defaults
  [site-defaults]
  (assoc-in
   site-defaults
   [:security :anti-forgery]
   {:error-response
    {:status 400
     :headers {"Content-Type" "text/html"}
     :body (cvd/message "Error" "Invalid Anti-Forgery Token")}}))

