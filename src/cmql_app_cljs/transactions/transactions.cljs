(ns cmql-app-cljs.transactions.transactions
  (:use cmql-core.operators.operators
        cmql-core.operators.qoperators
        cmql-core.operators.uoperators
        cmql-core.operators.stages)
  (:require cmql-core.operators.operators
            cmql-core.operators.qoperators
            cmql-core.operators.uoperators
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
            [cmql-js.util :refer [js-async-fn] :refer-macros [golet cmql]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [cljs.reader :refer [read-string]]
            cljs.pprint
            [cmql-app-cljs.quickstart.methods :as quick]))

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