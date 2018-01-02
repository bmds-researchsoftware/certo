(defproject certo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.6.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]                 
                 [org.clojure/java.jdbc "0.7.5"]
                 [org.postgresql/postgresql "9.4.1212"]]
  :main ^:skip-aot certo.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/java.classpath "0.2.3"]]}}
  :jvm-opts ["-Dclojure.server.repl={:port 5555 :accept clojure.core.server/repl}"])
