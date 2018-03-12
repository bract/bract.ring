(ns bract.ring.server
  "This namespace defines functions that start popular Ring servers, returning stopper function. Server dependencies
  are not included - you must include them in the project.")


(defn start-jetty-server
  "Start Jetty server using ring-jetty adapter. Include [ring/ring-jetty-adapter \"version\"] in your dependencies."
  [handler options]
  (let [form `(do
                (require 'ring.adapter.jetty)
                (import 'org.eclipse.jetty.server.Server)
                (let [^org.eclipse.jetty.server.Server server# (ring.adapter.jetty/run-jetty
                                                                 ~handler ~options)]
                  (fn [] (.stop server#))))]
    (eval form)))


(defn start-aleph-server
  "Start Aleph server. Include [aleph \"version\"] in your dependencies."
  [handler options]
  (let [form `(do
                (require 'aleph.http)
                (import 'java.io.Closeable)
                (let [^java.io.Closeable server# (aleph.http/start-server ~handler ~options)]
                  (fn [] (.close server#))))]
    (eval form)))


(defn start-http-kit-server
  "Start HTTP-Kit server. Include [http-kit \"version\"] in your dependencies."
  [handler options]
  (let [form `(do
                (require 'org.httpkit.server)
                (org.httpkit.server/run-server ~handler ~options))]
    (eval form)))
