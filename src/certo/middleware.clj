(ns certo.middleware
  (:require
   [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace wrap-stacktrace-log wrap-stacktrace-web]]
   [ring.middleware.ssl]
   [certo.auth :as ca]
   [certo.views.core :as cvd]))


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


(defn wrapped-handler [handler db md]
  (-> (handler db md)
      (wrap-defaults (customize-site-defaults site-defaults))
      (wrap-basic-authentication (partial ca/authenticated? db))
      ;; (wrap-content-type)
      ;; (wrap-not-modified)
      ;; (wrap-stacktrace)
      wrap-stacktrace-log
      ;; wrap-postgres-exception
      wrap-postgres-exception-web
      ;; wrap-exception
      wrap-exception-web))

