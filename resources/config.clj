{:database
 {:db-spec
  {:dbtype "postgresql"
   :dbname "certo"
   :host "localhost"
   :port "5432"
   :user "certo"
   :ssl true
   :sslfactory "org.postgresql.ssl.NonValidatingFactory"}
  :pgpass "./resources/db/.pgpass"}

  :webapp
 {:owner "Certo"
  :port 8080}}

