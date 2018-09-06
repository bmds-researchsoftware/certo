(ns certo.middleware
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]
   [clj-stacktrace.repl]
   [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults secure-api-defaults secure-site-defaults]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace wrap-stacktrace-log wrap-stacktrace-web]]
   [ring.middleware.ssl :refer [wrap-forwarded-scheme wrap-hsts wrap-ssl-redirect]]
   [certo.auth :as ca]
   [certo.views.core :as cvd]))


(defn wrap-whitelist [handler md]
  (fn [request]
    (let [[schema table] (drop 1 (str/split (:uri request) #"/"))]
      (if (and
           (not (contains? (:whitelist md) (:uri request)))
           (not (contains? (:whitelist md) (str schema "/" table))))
        (cvd/not-found request)
        (handler request)))))


(defn wrap-stacktrace-log-certo [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (println)
        (clj-stacktrace.repl/pst e)
        (throw e)))))


(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "text/plain"}
         :body (format "Error: %s" (.getMessage e))}))))


;; (defn wrap-exception-web [handler]
;;   (fn [request]
;;     (try
;;       (handler request)
;;       (catch Exception e
;;         (cvd/message "Error" (.getMessage e))))))


(defn wrap-exception-web [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "text/html"}
         :body (cvd/message "Error" (.getMessage e))}))))


(defn wrap-postgres-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        {:status 400
         :headers {"Content-Type" "text/plain"}
         :body (format "Database Error: %s" (.getMessage e))}))))


;; (defn wrap-postgres-exception-web [handler]
;;   (fn [request]
;;     (try
;;       (handler request)
;;       (catch org.postgresql.util.PSQLException e
;;         (cvd/message "Database Error" (.getMessage e))))))


(defn wrap-postgres-exception-web [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        {:status 400
         :headers {"Content-Type" "text/html"}
         :body (cvd/message "Database Error" (.getMessage e))}))))


(defn customize-site-defaults [site-defaults]
  (-> site-defaults
      (assoc :proxy true)
      (assoc-in
       [:security :anti-forgery]
       {:error-response
        {:status 400
         :headers {"Content-Type" "text/html"}
         :body (cvd/message "Error" "Invalid Anti-Forgery Token")}})))


(defn wrapped-handler [handler db md]
  (-> (handler db md)
      (wrap-defaults (customize-site-defaults secure-site-defaults))
      ;; (wrap-forwarded-scheme)
      (wrap-whitelist md)
      (wrap-basic-authentication (partial ca/authenticated? db))
      ;; (wrap-content-type)
      ;; (wrap-not-modified)
      ;; (wrap-stacktrace)
      ;; (wrap-stacktrace-log)
      (wrap-stacktrace-log-certo)
      ;; (wrap-postgres-exception)
      (wrap-postgres-exception-web)
      ;; (wrap-exception)
      (wrap-exception-web)))

