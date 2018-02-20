(ns qualified.core
  (:require
   [compojure.core :refer [GET POST] :as compojure]   
   [compojure.route :as route]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace wrap-stacktrace-log wrap-stacktrace-web]]
   [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
   [certo.auth :as ca]   
   [certo.controllers.default :as ccd]
   [certo.views.default :as cvd]   
   [certo.middleware :as cm]))


(defonce system-name "certo")


(defn handler [db md]
  (compojure/routes

   ;; (controllers/subjects/sample-handler db)
   ;; (qualified.controllers.sample/sample-handler db)

   ;; (qualified.controllers.sample/sample-handler db)

   ;; (compojure/GET
   ;;  "/"
   ;;  []
   ;;  {:body "sample"
   ;;   :status 200
   ;;   :headers {"Content-Type" "text/plain"}})
   
   ;; (compojure/routes
   ;;  (compojure/GET
   ;;   "/"
   ;;   []
   ;;   {:body "sample"
   ;;    :status 200
   ;;    :headers {"Content-Type" "text/plain"}}))
   
    (ccd/default-handler db md)))


(defn wrapped-handler [db md]
  (-> (handler db md)

      (wrap-defaults (cm/customize-site-defaults site-defaults))

      (wrap-basic-authentication (partial ca/authenticated? db))
      
      ;; (wrap-content-type)
      ;; (wrap-not-modified)
      ;; (wrap-stacktrace)
      wrap-stacktrace-log
      ;; cm/wrap-postgres-exception
      cm/wrap-postgres-exception-web      
      ;; cm/wrap-exception
      cm/wrap-exception-web))


;; (defn -main [& args]
;;   (let [system-name "qualified"
;;         system (system/new-system system-name)]
;;     (component/start system)))

