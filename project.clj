(defproject hs2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
          [org.clojure/clojure "1.5.1"]
          [org.clojure/data.xml "0.0.7"]
          [org.clojure/data.zip "0.1.1"]
          [http-kit "2.1.13"]
          [compojure "1.1.6"]
          [ring "1.2.1"]
          [incanter "1.5.4"]
          ;[incanter/incanter-charts "1.5.4"]
          ;[incanter/incanter-mongodb "1.5.4"]
          [congomongo "0.4.1"]
          [clj-time "0.6.0"]
          ;[clojure-csv/clojure-csv "2.0.1"]
  ]
  :main hs2.core
)
