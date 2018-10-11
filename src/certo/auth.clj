(ns certo.auth
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.pprint :as pprint]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as credentials])))


;; Used for form based authentication
(defn load-user-record [db username]
  (if-let [user-record
           (jdbc/query
            db
            ;; password is hashed using crypt(password, gen_salt('bf', 8))
            ["select username, password, usergroup from sys.users where username=?" username]
            {:row-fn (fn [row] {:username (:username row) :password (:password row) :roles #{(:usergroup row)}})
             :result-set-fn first})]
    (workflows/make-auth user-record)
    nil))


;; Used for basic authentication
;; (defn authenticated? [db name pass]
;;   (let [credentials
;;         (jdbc/query
;;          db
;;          ["select username, password from sys.users"]
;;          {:row-fn (juxt :username :password) :result-set-fn set})]
;;     (first (get credentials [name pass]))))

