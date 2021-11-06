(ns cmql-app-cljs.core
  (:use cmql-core.operators.operators
        cmql-core.operators.stages)
  (:require cmql-core.operators.operators
            cmql-core.operators.options
            cmql-core.operators.stages
            [cljs.core.async :refer [go go-loop <! chan close! take!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cmql-js.cmql-arguments :refer-macros [p f qf] :refer [o d]]
            [cmql-js.driver.cursor :refer [c-take-all c-print-all]]
            [cmql-js.driver.cursor :refer-macros [c-take-all!]]
            [cmql-js.driver.settings :refer [update-defaults defaults]]
            [cmql-js.driver.client :refer [create-mongo-client]]
            [cmql-js.commands :refer-macros [q fq insert insert! delete! dq]]
            [cmql-js.util :refer [run-query] :refer-macros [golet cmql]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [cljs.reader :refer [read-string]]
            cljs.pprint
            [cmql-app-cljs.quickstart.methods :as quick]))

(defn main [])

(update-defaults :connection-string "mongodb://localhost:27017")
(update-defaults :client (create-mongo-client) :decode "cljs")

;;tutorial is made using, cljs-maps as return values that are printed
;;if "js" was wanted change :decode "js" (this is the default) alternatively decode can be setted at query call time
;; like have "cljs" default but return for a query "js" (in all cases we can do clj->js or js->clj even if we have them
;; in wrong format)

(def mongodb (js/require "mongodb"))

(def MongoClient (.-MongoClient mongodb))

;;exec query is like this, to allow cb call or promise return
#_(defn exec-query
  ([f cb] (take! (f) (fn [r] (cb r))))
  ([f] (js/Promise. (fn [resolve _] (go (resolve (<! (f))))))))

(defn clear-data []
  (quick/clear-data))

(defn list-databases
  ([cb] (run-query quick/list-databases cb))
  ([] (run-query quick/list-databases)))

(defn insert-data
  ([cb] (run-query quick/insert-data cb))
  ([] (run-query quick/insert-data)))

(defn update-data
  ([cb] (run-query quick/update-data cb))
  ([] (run-query quick/update-data)))

(defn aggregate-data
  ([cb] (run-query quick/aggregate-data cb))
  ([] (run-query quick/aggregate-data)))

(defn delete-data
  ([cb] (run-query quick/delete-data cb))
  ([] (run-query quick/delete-data)))

(go (clear-data)
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