(ns hs2.core
	(:use [compojure.route :only [files not-found]]
    [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
    [compojure.core :only [defroutes GET POST DELETE ANY context]]
    org.httpkit.server)
  
  (:use [clojure.data.zip.xml :only (attr text xml->)]) 
  (:require [clojure.zip :as zip]                                                     
    [clojure.xml :as xml]) 

   (:use incanter.core)
   (:use incanter.stats)
   (:use incanter.charts)
   (:use incanter.io) 
   (:use incanter.mongodb)
   (:use somnium.congomongo)
   

  (:require [clj-time 
              [core :as tm]
              [local :as loc]
              [format :as fmt]])
  (:use clj-time.coerce)

  (:require [clojure.java.io :as io]); avoids 'copy' clasking with incanter.core 'copy'
  (:gen-class :main true)
)

(defn zip-str2 [s]
  (zip/xml-zip s))

(defn hex->num [#^String s]
 (binding [*read-eval* false]
   (let [n (read-string s)]
     (when (number? n)
       n))))

(defn parse-int [s]
 (Integer. (re-find  #"\d+" s )))

(defn hex2int [hexs]
  (let [sint (hex->num hexs)]
    (if ( < sint (hex->num "0x80000000"))
     sint ( - ( - sint (hex->num "0xFFFFFFFF") ) 1))))

(def timestamp (ref 0))
(def last-demand (ref 0))
(def demand (ref 0))
(def received (ref -1))
(def delivered (ref -1))
(def devicemacid (ref "devicemacid"))
(def metermacid (ref "metermacid"))
(def macid (ref "macid"))
(def changed-demand (ref false))
(def debug-parse (ref false))


(defn receive-eagle-post [req] ;; ordinary clojure function, accepts a request map, returns a response map

  (with-channel req channel 
    (let [content-type  (-> req :params :content-type)
        headers (-> req :params :headers)    ; param from uri
        content-length (-> req :params :content-length)] ; form param
        ; {:status  200
        ;   :headers {"Content-Type" content-type}}
        (send! channel {:status 200
         :headers {"Content-Type" content-type}}) 
    )
    (let [input-xml (get req :body)]
      (let [xml(xml/parse input-xml)]

        (def zipped (zip/xml-zip xml))
  
        (try
          (let [str-count (count (apply str (get (get xml :attrs) :macId )))]
            (if ( = str-count 18 )

              (dosync
                (ref-set macid (apply str (get (get xml :attrs) :macId ))))
              ) 
            ) 
          (catch Exception e (if @debug-parse (println "Caught metermacid" (.getMessage e)))  "failure")
        ) 
        (try
          (dosync
            (ref-set timestamp (parse-int(get (get xml :attrs) :timestamp ))))
          (catch Exception e (if @debug-parse (println "Caught timestamp" (.getMessage e)))  "failure")
        ) 
        (try
          (dosync
            (ref-set delivered (hex2int (apply str (xml-> zipped :CurrentSummationDelivered :SummationDelivered text)))))
          (catch Exception e (if @debug-parse (println "Caught delivered" (.getMessage e)))  "failure")
        ) 

        (try
          (dosync
            (ref-set demand (hex2int (apply str (xml-> zipped :InstantaneousDemand :Demand text))))
            ;(println @demand)
            (if (not= @last-demand @demand)
              (do
                (ref-set changed-demand true)
                (if @debug-parse (println "New Demand!"))
              )
            )
          )

          (catch Exception e (if @debug-parse (println "Caught new demand" (.getMessage e)))  "failure")
        ) 

        (try
          (dosync
            (ref-set received (hex2int (apply str (xml-> zipped :CurrentSummationDelivered :SummationReceived text)))))
          (catch Exception e (if @debug-parse (println "Caught received" (.getMessage e)))  "failure")
        ) 

        (try
          (let [str-count (count (apply str (xml-> zipped :InstantaneousDemand :DeviceMacId text)))]
            (if ( = str-count 18 )

              (dosync
                (ref-set devicemacid (apply str (xml-> zipped :InstantaneousDemand :DeviceMacId text))))
              ) 
            ) 
          (catch Exception e (if @debug-parse (println "Caught devicemacid" (.getMessage e)))  "failure")
        ) 

        (try
          (let [str-count (count (apply str (xml-> zipped :InstantaneousDemand :MeterMacId text)))]
            (if ( = str-count 18 )

              (dosync
                (ref-set metermacid (apply str (xml-> zipped :InstantaneousDemand :MeterMacId text))))
              ) 
            ) 
          (catch Exception e (if @debug-parse (println "Caught metermacid" (.getMessage e)))  "failure")
        )  
      )
    )
  )
  (print "Date-Time: ") (println (loc/to-local-date-time (from-long (* @timestamp 1000))))
  (print "Demand: ") (println @demand)
  (print "Received: ") (println @received)
  (print "Delivered: ") (println @delivered)
  (print "DeviceMacId: ") (println @devicemacid)
  (print "Timestamp: ") (println @timestamp)
  (def date-formatter (fmt/formatter "yyyy-MM-dd"))
  (def time-formatter (fmt/formatter "HH:mm:ss"))
  (def timezonestr "Australia/Melbourne")
  (let 
    [    
      stamptime (from-long (* @timestamp 1000)) comptime (tm/now)

      localstampdate (fmt/unparse (fmt/with-zone (:date fmt/formatters)
                (tm/time-zone-for-id timezonestr)) stamptime)

      localstamptime (fmt/unparse (fmt/with-zone (:time-no-ms fmt/formatters)
                (tm/time-zone-for-id timezonestr)) stamptime)]

    (print "Date: ") (println localstampdate)
    (print "Time: ") (println (clojure.string/replace localstamptime #"\+" ", +"))
  
    (println)
    (if (= @changed-demand true)       
      (if (> @delivered -1)
        (dosync
          (ref-set last-demand @demand)
          (ref-set changed-demand false)
          (spit "eagle.csv" (reduce str [ localstampdate ", " (clojure.string/replace localstamptime #"\+" ", +") ", " 
            (str @timestamp) ", " (str @demand) ", " (str @delivered) ", " (str @received) ", " 
            (str @devicemacid) ", " (str @metermacid) ", " 
            (str @macid) "\n"]) :append true) 
        )      
      )
    )
  )
)


(defroutes all-routes 
  (POST "/" [] receive-eagle-post)  
) 

(defn -main 
  "Eagle POST recevier and csv generator."
  []

  (if (.exists (io/as-file "eagle.csv"))
    (println "eagle.csv data file exists")
    ; else ...
    (do     
      (println "Creating eagle.csv data file")
      (spit "eagle.csv" 
        (reduce str ["localstampdate, localstamptime, tzone, timestamp, demand, delivered, "
        "received, devicemacid, metermacid, macid \n"]))))

  (println "Eagle POST recevier and csv generator. Listening for post on port 8085. \n\r")
  (println "Set Cloud on Eagle to Manual and enter the IP of your PC, eg http://192.168.1.10:8085\n\r")
  (println "Waiting for POST from Eagle, results will be written to 'eagle.csv' ...\n\r")

  (run-server (site #'all-routes) {:port 8085})

  ; (def data
  ;   (read-dataset
  ;     "/Users/gordon/cars.csv"
  ;     :header true))
  (def edata
    (read-dataset
      "/Users/gordon/ClojProj/hs2/eagle.csv"
      :header true))

    (print edata); works

  ; (with-data data
  ;   (def lm (linear-model ($ :dist) ($ :speed)))
  ;   (doto (scatter-plot ($ :speed) ($ :dist))
  ;     (add-lines ($ :speed) (:fitted lm))
  ;     view))
  (with-data edata
    ;(def lm (linear-model ($ :timestamp) ($ :delivered)))
    (doto (scatter-plot ($ :delivered) ($ :timestamp))
      (add-lines ($ :delivered) ($ :received)); (:fitted lm))
      view))

  ;(def results (-> (conj-cols data (:fitted lm) (:residuals lm))
   ;(col-names [:speed :dist :predicted :residuals])))
  ; (def results (-> (conj-cols edata (:fitted lm) (:residuals lm))
  ;  (col-names [:demand :timestamp :predicted :residuals])))

  ; ;(println results)
  ; (println results)


  

  
)


