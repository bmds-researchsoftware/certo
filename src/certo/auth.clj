(ns certo.auth
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.pprint :as pprint]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as credentials])))


(defn load-user-record [db username]
  (if-let [user-record
           (jdbc/query
            db
            ;; TO DO: Don't call credentials/hash-bcrypt on the password, it should be returned in that format
            ["select username, password, usergroup from sys.users where username=?" username]
            {:row-fn (fn [row] {:username (:username row) :password (credentials/hash-bcrypt (:password row)) :roles #{(:usergroup row)}})
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

