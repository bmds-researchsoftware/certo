# Certo
Certo is a Clojure library for developing data-driven web applications
whose design was influenced by event sourcing and domain-driven
design.  All data modifying operations are recorded as events, and the
availability of operations is specified by a flexible precedence
hierarchy.


# Production
lein clean
lein uberjar
java -jar target/uberjar/certo-0.1.0-SNAPSHOT-standalone.jar

