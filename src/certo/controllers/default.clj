(ns certo.controllers.default
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]   
   [clojure.edn :as edn]
   [cemerick.friend :as friend]
   [compojure.core :refer [GET POST ANY] :as compojure]
   [compojure.route :as route]
   [ring.util.response :refer [redirect]]
   [certo.models.default :as model :refer [insert!]]
   [certo.views.core :as view]
   [certo.metadata :as metadata]
   [certo.utilities :as cu]))


;; text primary keys can contains lower case letters, numbers, hyphens, periods, and underscores without whitespace
(def uuid-or-integer-or-text-id "/:id{[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[0-9]+|[a-zA-Z0-9\\-_\\.)]+}")


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
    "/login"
    req
    (view/login (:system-name md)))

   (friend/logout (ANY "/logout" request (ring.util.response/redirect "/login")))

   ;; display system dashboard if there is no app dashboard
   (compojure/GET
    "/"
    {session :session :as req}
    (friend/authenticated (view/dashboard (:system-name md) (model/user db (cu/authenticated-username req)) (model/dashboard db))))

   (compojure/GET
    "/dashboard"
    {session :session :as req}
    (friend/authenticated (view/dashboard (:system-name md) (model/user db (cu/authenticated-username req)) (model/dashboard db))))
   
   ;; Match all routes of the form "/:schema/:table"
   ;; For example: http://example.com/sys/fields
   (compojure/context

    "/:schema/:table"

    [schema table]

    (let [ ;; TO DO: You can pass parameters into new, e.g. you could put a combo box, labeled "Add Event Class Field", that
          ;; contains a list of all schema.table on the event class form.  When you pick a schema.table, the insert Event
          ;; Class Fields form is opened, but the list of fields is only those for the schema.table.
          table-map
          (if (= schema "event")
            (model/event-classes db table)
            (model/tables db schema table))
          fields (model/fields db schema table table-map)]

      (friend/authenticated
       
       (compojure/routes

        (compojure/GET
         "/"
         {query-params :query-params {referer "referer"} :headers :as req}
         (let [op (get query-params "op")]
           (when (and (not (nil? op)) (not= op "edit") (not= op "show"))
             (throw (Exception. (format "Illegal op: %s" op))))

           ;; TO DO: Must change model/select so that it uses a reducible-query.

           (if (not (cu/clean-get-url? req))
             (ring.util.response/redirect (cu/clean-get-url req))
             (let [{rs :result-set cnt :count cnt-all :count-all} (model/select db fields table-map schema table (dissoc query-params "op") true)
                   cnt-one? (= (count (take 2 rs)) 1)]
               (cond
                 (and cnt-one? (= op "edit"))
                 (view/edit fields schema table referer (first rs))
                 (and cnt-one? (= op "show"))
                 (view/show fields schema table referer (first rs))
                 :else (view/table table-map fields schema table rs cnt cnt-all (cu/base-url req) (dissoc query-params "op")))))))

        (compojure/GET
         "/new"
         {query-params :query-params {referer "referer"} :headers :as req}
         (view/new fields schema table referer

                   ;; TO DO: Change so only update username and event_date and event_time only if they don't already appear in query-string

                   ;; TO DO: Update form view select-result so that you even if you pass in the username, date, and time, that you get a
                   ;; username select-option, date control, time control so you can change the values.

                   (if (= schema "event")
                     (assoc
                      query-params
                      ;; (str schema "." table ".event_classes_id") table
                      (str schema "." table ".event_by") (cu/authenticated-username req)
                      (str schema "." table ".event_date") (str (certo.utilities/date-now))
                      ;; TO DO: If is_time_required then add it 
                      )
                     query-params)))

        (compojure/POST
         "/"
         {params :params :as req}
         (model/insert! db md fields table-map schema table
                        (assoc params
                               (model/stf schema table "created_by") (cu/authenticated-username req)
                               (model/stf schema table "updated_by") (cu/authenticated-username req)))
         (redirect (:redirect-to params) :see-other))

        (compojure/GET
         uuid-or-integer-or-text-id
         {{id :id} :params {referer "referer"} :headers}
         (view/show fields schema table referer (model/select-by-id db fields schema table id)))

        ;; TO DO: postgresql function should take one parameter, i.e. the parent id

        (compojure/GET
         (str uuid-or-integer-or-text-id "/edit")
         {{id :id} :params {referer "referer"} :headers}
         (view/edit fields schema table referer (model/select-by-id db fields schema table id)))

        (compojure/PUT
         uuid-or-integer-or-text-id
         {{id :id} :params params :params :as req}
         ;; Note: discard :id from params passes it in as an argument,
         ;; its value is the current value of the primary key, and is
         ;; used in the update statement's where clause.  A potentially
         ;; different value for the primary key is held in params.
         ;; This approach allows us to update the value of a row's
         ;; primary key.
         (model/update! db fields schema table
                        (-> params
                            (dissoc :id)
                            (assoc (model/stf schema table "updated_by") (cu/authenticated-username req)))
                        id)
         (redirect (:redirect-to params) :see-other))

        (compojure/DELETE
         uuid-or-integer-or-text-id
         {{id :id} :params params :params}
         (model/delete! db fields schema table id)
         (redirect (:redirect-to params) :see-other))))))

   ;; Use these for debugging
   ;; (compojure/POST "/" req (show-request schema table req))
   ;; (compojure/GET "/" req (show-request schema table req))
   
   (route/not-found view/not-found)))

