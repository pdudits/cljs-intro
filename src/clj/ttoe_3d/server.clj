(ns ttoe-3d.server
  (:require [ttoe-3d.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
     (run-jetty app {:port port :join? false})))
