(ns certo.controllers.default
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]   
   [clojure.edn :as edn]
   [compojure.core :refer [GET POST] :as compojure]
   [compojure.route :as route]
   [ring.util.response :refer [redirect]]
   
   [certo.models.default :as model]
   [certo.views.default :as view]))


(def uuid-or-integer-pk "/:pk{[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[0-9]+}")


;; For debugging
(defn show-request [schema table req]
  {:body (format "%s %s %s" schema table (with-out-str (pprint/pprint req)))
   :status 200
   :headers {"Content-Type" "text/plain"}})


(defn redirect-to-schema-table-root [schema table]
  (redirect (str "/" schema "/" table) :see-other))


(defn default-handler [db md]
  (compojure/routes

   (compojure/GET
    "/"
    []
    (view/dashboard "Certo" (model/dashboard db md)))

   ;; Match all routes of the form "/:schema/:table"
   ;; For example: http://example.com/sys/fields
   (compojure/context
    ;; routes-regex is a whitelist of schema/table pairs
    (format "/:schema-table{%s}" (:routes-regex md))

    [schema-table]

    (let [[schema table] (str/split schema-table #"/")]

      (compojure/routes
     
       (compojure/GET
        "/"
        []
        (view/table (:fields md) schema table (model/select-all db md schema table)))
       
       (compojure/GET
        "/new"
        []
        (view/new (:fields md) schema table))
       
       (compojure/POST
        "/"
        {params :params username :basic-authentication}
        (model/insert! db md schema table
                       (assoc params
                              (model/stf schema  table "created_by") username
                              (model/stf schema table "updated_by") username))
        (redirect-to-schema-table-root schema table))
       
       (compojure/GET
        uuid-or-integer-pk
        [pk]
        (view/show (:fields md) schema table (model/select-one db md schema table pk)))
       
       (compojure/GET
        (str uuid-or-integer-pk "/edit")
        [pk]
        (view/edit (:fields md) schema table (model/select-one db md schema table pk)))
       
       (compojure/PUT
        uuid-or-integer-pk
        {params :params username :basic-authentication}
        ;; note: discards the :pk in query-params but keeps the one in form-params
        (model/update! db md schema table
                       (-> params
                           (dissoc :pk)
                           (assoc (model/stf schema table "updated_by") username)))
        (redirect-to-schema-table-root schema table))
       
       (compojure/DELETE
        uuid-or-integer-pk
        [pk]
        (model/delete! db md schema table pk)
        (redirect-to-schema-table-root schema table)))))

   ;; Use these for debugging
   ;; (compojure/POST "/" req (show-request schema table req))
   ;; (compojure/GET "/" req (show-request schema table req))
   
   (route/not-found view/not-found)))

