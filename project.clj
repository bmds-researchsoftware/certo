(defproject certo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.csv "0.1.4"]
                 [ring-basic-authentication "1.0.5"]
                 [metosin/jsonista "0.2.1"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.layerware/hugsql "0.4.8"]
                 [compojure "1.6.1"]
                 [hiccup "1.0.5"]
                 [eftest "0.5.2"]
                 [ring/ring-core "1.7.0-RC1"]
                 [ring/ring-devel "1.7.0-RC1"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-jetty-adapter "1.7.0-RC1"]
                 [ring/ring-ssl "0.3.0"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.postgresql/postgresql "42.2.2"]
                 [clojure.java-time "0.3.2"]]
  :local-repo ".m2")

