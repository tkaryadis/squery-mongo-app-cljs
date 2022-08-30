(ns squery-mongo-app-cljs.core
  (:use squery-mongo-core.operators.operators
        squery-mongo-core.operators.qoperators
        squery-mongo-core.operators.uoperators
        squery-mongo-core.operators.stages)
  (:require squery-mongo-core.operators.operators
            squery-mongo-core.operators.qoperators
            squery-mongo-core.operators.uoperators
            squery-mongo-core.operators.options
            squery-mongo-core.operators.stages
            [cljs.core.async :refer [go go-loop <! chan close! take!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [squery-mongo.squery-arguments :refer-macros [p f qf u] :refer [o d]]
            [squery-mongo.driver.cursor :refer [c-take-all c-print-all]]
            [squery-mongo.driver.cursor :refer-macros [c-take-all! c-print-all!]]
            [squery-mongo.driver.settings :refer [update-defaults defaults]]
            [squery-mongo.driver.client :refer [create-mongo-client]]
            [squery-mongo.commands :refer-macros [q fq insert insert! delete! dq]]
            [squery-mongo.util :refer [js-async] :refer-macros [golet squery]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [cljs.reader :refer [read-string]]
            cljs.pprint
            [squery-mongo-app-cljs.quickstart.methods :as quick]))

(defn main [])

(update-defaults :connection-string "mongodb://localhost:27017")
(update-defaults :client (create-mongo-client) :decode "cljs")

;;tutorial is made using, cljs-maps as return values that are printed
;;if "js" was wanted change :decode "js" (this is the default) alternatively decode can be setted at query call time
;; like have "cljs" default but return for a query "js" (in all cases we can do clj->js or js->clj even if we have them
;; in wrong format)

(def mongodb (js/require "mongodb"))

(def MongoClient (.-MongoClient mongodb))

;;test pipeline update not supported in nodejs driver
(go (try
      (let [client (<p! (.connect (defaults :client)))
            coll (.collection (.db client "testdb") "testcoll")
            _ (try (<p! (.drop coll)) (catch :default e e))
            _ (insert! :testdb.testcoll {:a 1})
            _ (prn "before update")
            _ (c-print-all! (q :testdb.testcoll))
            _ (<p! (.updateOne coll
                               #js {}
                               (u (set!- :a 10))))
            _ (prn "after update")
            _ (c-print-all! (q :testdb.testcoll))
            _ (.exit js/process)
            ]
        "done")
      (catch :default e (prn (.toString e)))))




;;exec query is like this, to allow cb call or promise return
#_(defn exec-query
  ([f cb] (take! (f) (fn [r] (cb r))))
  ([f] (js/Promise. (fn [resolve _] (go (resolve (<! (f))))))))

(defn clear-data []
  (quick/clear-data))

(defn list-databases
  ([cb] (js-async quick/list-databases cb))
  ([] (js-async quick/list-databases)))

(defn insert-data
  ([cb] (js-async quick/insert-data cb))
  ([] (js-async quick/insert-data)))

(defn update-data
  ([cb] (js-async quick/update-data cb))
  ([] (js-async quick/update-data)))

(defn aggregate-data
  ([cb] (js-async quick/aggregate-data cb))
  ([] (js-async quick/aggregate-data)))

(defn delete-data
  ([cb] (js-async quick/delete-data cb))
  ([] (js-async quick/delete-data)))

#_(go (clear-data)
    (<p! (list-databases))
    (<p! (insert-data))
    (<p! (update-data))        ;;requires the insert
    (<p! (delete-data))        ;;requires insert+update(the deleted ones are the ones that we updated)

    ;; before doing the aggregation
    ;;get the atlas sample data from https:github.com/huynhsamha/quick-mongo-atlas-datasets
    ;;quick-mongo-atlas-datasets-master/dump$ mongorestore sample_airbnb/.
    ;;(install mongorestore if you dont have it https:docs.mongodb.com/database-tools/installation/installation-linux/)

    ;(<p! (aggregate-data))
    )