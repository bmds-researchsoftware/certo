(ns certo.auth
  (:require [clojure.java.jdbc :as jdbc]))


(defn authenticated? [db name pass]
  (let [credentials
        (jdbc/query
         db
         ["select username, password from sys.users"]
         {:row-fn (juxt :username :password) :result-set-fn set})]
    (first (get credentials [name pass]))))

