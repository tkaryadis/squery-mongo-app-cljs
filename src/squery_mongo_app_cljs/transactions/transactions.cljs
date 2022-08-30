(ns squery-mongo-app-cljs.transactions.transactions
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
            [squery-mongo.squery-arguments :refer-macros [p f qf] :refer [o d]]
            [squery-mongo.driver.cursor :refer [c-take-all c-print-all]]
            [squery-mongo.driver.cursor :refer-macros [c-take-all!]]
            [squery-mongo.driver.settings :refer [update-defaults defaults]]
            [squery-mongo.driver.client :refer [create-mongo-client]]
            [squery-mongo.commands :refer-macros [q fq insert insert! delete! dq]]
            [squery-mongo.util :refer [js-async-fn] :refer-macros [golet squery]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [cljs.reader :refer [read-string]]
            cljs.pprint
            [squery-mongo-app-cljs.quickstart.methods :as quick]))

(defn main [])

(update-defaults :connection-string
                 "mongodb://myuser:mypass@localhost:27011/?authSource=admin&replicaSet=myrepl&readPreference=primary&appname=MongoDB%20Compass&ssl=false")
(update-defaults :client (create-mongo-client) :decode "cljs")

(def mongodb (js/require "mongodb"))

(def MongoClient (.-MongoClient mongodb))

(golet [client (<p! (.connect (defaults :client)))
        coll1 (-> (.db client "mydb1")
                  (.collection "foo"))
        coll2 (-> (.db client "mydb2")
                  (.collection "bar"))
        _ (try (<p! (.drop coll1))  (catch :default e e))
        _ (try (<p! (.drop coll2))  (catch :default e e))

        ;;------i can create collections inside transactions also >= 4.4
        ;;but i dont do it if no reason to so here i create them before
        _ (<p! (.insertOne coll1
                           #js { "abc" 0 }
                           #js { "writeConcern"  { "w" "majority"} }))
        _ (<p! (.insertOne coll2
                           #js { "xyz" 0 }
                           #js { "writeConcern"  { "w" "majority"} }))

        ;;--------------Transaction-----------------------------------

        session (.startSession (defaults :client))

        toptions #js {"readPreference" "primary",
                      "readConcern"    {"level" "local"},
                      "writeConcern"   {"w" "majority"}
                      }

        _ (<p! (.withTransaction
               session
               (js-async-fn (fn []
                              (go (<p! (.insertOne coll1 #js {:abc 2} #js {:session session}))
                                  (<p! (.insertOne coll2 #js {:xyz 1000} #js {:session session})))))
               toptions))]
  "done")