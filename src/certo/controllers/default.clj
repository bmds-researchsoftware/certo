(ns certo.controllers.default
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pprint]   
   [clojure.edn :as edn]
   [compojure.core :refer [GET POST] :as compojure]
   [compojure.route :as route]
   [ring.util.response :refer [redirect]]
   [certo.models.default :as model]
   [certo.views.core :as view]
   [certo.metadata :as metadata]
   [certo.models.events :as me]))


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

   ;; display system dashboard if there is no app dashboard
   (compojure/GET
    "/"
    {username :basic-authentication}
    (view/dashboard (:system-name md) (model/user db username) (model/dashboard db)))

   (compojure/GET
    "/dashboard"
    {username :basic-authentication}
    (view/dashboard (:system-name md) (model/user db username) (model/dashboard db)))
   
   ;; Match all routes of the form "/:schema/:table"
   ;; For example: http://example.com/sys/fields
   (compojure/context

    "/:schema/:table"

    [schema table]

    (let [;; TO DO: You can pass parameters into new, e.g. you could put a combo box, labeled "Add Event Class Field", that
          ;; contains a list of all schema.table on the event class form.  When you pick a schema.table, the insert Event
          ;; Class Fields form is opened, but the list of fields is only those for the schema.table.
          table-map
          (if (= schema "event")
            (model/event-classes db table)
            (model/tables db schema table))
          fields (model/fields db schema table table-map)]

      (compojure/routes

       (compojure/GET
        "/"
        {query-params :query-params {referer "referer"} :headers}
        (let [op (get query-params "op")]
          (when (and (not (nil? op)) (not= op "edit") (not= op "show"))
            (throw (Exception. (format "Illegal op: %s" op))))

          ;; TO DO: Must change model/select so that it uses a reducible-query.

          (let [rs (model/select db fields table-map schema table (dissoc query-params "op"))
                cnt (count (take 2 rs))]
            (cond
              (= cnt 0) (throw (Exception. "None found"))
              (and (= cnt 1) (= op "edit"))
              (view/edit fields schema table referer (first rs))
              (and (= cnt 1) (= op "show"))
              (view/show fields schema table referer (first rs))
              :else (view/table table-map fields schema table rs (dissoc query-params "op"))))))

       (compojure/GET
        "/new"
        {query-params :query-params username :basic-authentication {referer "referer"} :headers}
        (view/new fields schema table referer

                  ;; TO DO: Change so only update username and event_date and event_time only if they don't already appear in query-string

                  ;; TO DO: Update form view select-result so that you even if you pass in the username, date, and time, that you get a
                  ;; username select-option, date control, time control so you can change the values.

                  (if (= schema "event")
                    (assoc
                     query-params
                     (str schema "." table ".event_classes_id") table
                     (str schema "." table ".event_by") username
                     (str schema "." table ".event_date") (str (certo.utilities/date-now))
                     ;; TO DO: If is_time_required then add it 
                     )
                    query-params)))
       
       (compojure/POST
        "/"
        {params :params username :basic-authentication}
        (model/insert! db md fields schema table
                       (assoc params
                              (model/stf schema table "created_by") username
                              (model/stf schema table "updated_by") username))
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
        {{id :id} :params params :params username :basic-authentication}
        ;; Note: discard :id from params passes it in as an argument,
        ;; its value is the current value of the primary key, and is
        ;; used in the update statement's where clause.  A potentially
        ;; different value for the primary key is held in params.
        ;; This approach allows us to update the value of a row's
        ;; primary key.
        (model/update! db fields schema table
                       (-> params
                           (dissoc :id)
                           (assoc (model/stf schema table "updated_by") username))
                       id)
        (redirect (:redirect-to params) :see-other))
       
       (compojure/DELETE
        uuid-or-integer-or-text-id
        {{id :id} :params params :params}
        (model/delete! db fields schema table id)
        (redirect (:redirect-to params) :see-other)))))

   ;; Use these for debugging
   ;; (compojure/POST "/" req (show-request schema table req))
   ;; (compojure/GET "/" req (show-request schema table req))
   
   (route/not-found view/not-found)))

