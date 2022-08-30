(ns squery-mongo-app-cljs.quickstart.methods
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
            [squery-mongo.util :refer [js-async] :refer-macros [golet squery]]
            [squery-mongo.driver.cursor :refer-macros [c-take-all! c-print-all!]]
            [squery-mongo.driver.settings :refer [update-defaults defaults]]
            [squery-mongo.driver.client :refer [create-mongo-client]]
            [squery-mongo.commands :refer-macros [q fq insert insert! update- uq delete! dq]]
            [cljs-bean.core :refer [bean ->clj ->js]]
            [cljs.reader :refer [read-string]]
            cljs.pprint))

;;-------------------list-databases-----------------------------------------------------------------

(defn clear-data []
  (golet [client (<p! (.connect (defaults :client)))
          coll (.collection (.db client "sample_airbnb") "listingsAndReviews")]
    (try (<p! (.drop coll)) (catch :default e e))))

;; i add (o)  o=> uses the default encode
(defn list-databases []
  (golet [client (<p! (.connect (defaults :client)))
          admin-db (.admin (.db client))
          databases (<p! (.listDatabases admin-db (o))) ;;o is options, added here to return clojure-maps
          ]
         (println "Databases")
         (dorun (map #(println " - " (get % :name)) (get databases :databases)))
         "done"))

(defn insert-data []
  (golet [doc1 {:name "Lovely Loft", :summary "A charming loft in Paris", :bedrooms 1, :bathrooms 1}
          docs #js [{"name"          "Infinite Views",
                     "summary"       "Modern home with infinite views from the infinity pool",
                     "property_type" "House",
                     "bedrooms"      5,
                     "bathrooms"     4.5,
                     "beds"          5}
                    {"name"          "Private room in London",
                     "property_type" "Apartment",
                     "bedrooms"      1,
                     "bathroom"      1}
                    {"name"        "Beautiful Beach House",
                     "summary"
                                   "Enjoy relaxed beach living in this house with a private beach",
                     "bedrooms"    4,
                     "bathrooms"   2.5,
                     "beds"        7,
                     "last_review" (js/Date.)}]
          client (<p! (.connect (defaults :client)))
          ;;client.db("sample_airbnb").collection("listingsAndReviews").insertOne(newListing);
          coll (.collection (.db client "sample_airbnb") "listingsAndReviews")
          r1 (<p! (.insertOne coll doc1))
          r2 (<p! (.insertMany coll docs))
          _ (println "New listing created with the following id:" (.-insertedId r1))
          _ (println (.-insertedCount r2) "New listing(s) created with the following id:" (.-insertedIds r2))
          ;_ (c-print-all! (q :sample_airbnb.listingsAndReviews))
          ]))


(defn read-data []
  (go (try
        (let [client (<p! (.connect (defaults :client)))
              coll (.collection (.db client "sample_airbnb") "listingsAndReviews")
              ;;findOne({ name: "Infinite Views" }
              nameOfListing "Infinite Views"
              ;;squery way, find like command call looks like aggregation
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews
                                 (=? :name nameOfListing)   ;;=- is the query operator
                                 (limit 1)))
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews
                                 (= :name nameOfListing)    ;;= is the aggregate operator, so $expr is used internally
                                 (limit 1)))
              ;; Drive interop way
              r (<p! (.findOne coll (d {:name nameOfListing}) (o))) ;;if i dont want cljs-map results, remove the (o)
              _ (if r
                  (do (println "Found a listing in the collection with the name : " nameOfListing)
                      (cljs.pprint/pprint r))
                  (println "No listings found with the name " nameOfListing))

              ;;Find
              minimumNumberOfBedrooms 4
              minimumNumberOfBathrooms 2
              maximumNumberOfResults 5

              ;;squery way find like aggregate, more intuative (will use the default decode, unless i say otherwise)
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews
                                 (>=? :bedrooms minimumNumberOfBedrooms)
                                 (>=? :bathrooms minimumNumberOfBathrooms)
                                 (sort :!last_review)
                                 (limit maximumNumberOfResults)))
              ;; instead of c-print-all! c-take-all!(get vector) or .toArray(get array)

              ;;nodejs tutorial has a more fancy print, here just print the docs
              _ (println "----------Find-results--------------")
              ;;driver way interop + squery arguments (f is for filter allowing us to use squery like filters)
              _ (c-print-all! (-> (.find coll
                                         (f (>=? :bedrooms minimumNumberOfBedrooms)
                                            (>=? :bathrooms minimumNumberOfBathrooms))
                                         ;(o)  without this we will get results in #js objects not clojure maps
                                         )
                                 (.sort (d {:last_review -1}))           ;;d will do clj->js, same as  #js {:last_review -1}
                                 (.limit maximumNumberOfResults)))
              _ (println "--------------------------")
              ]
          "done")
        (catch :default e (.toString e)))))

(defn update-data []
  (go (try
        (let [client (<p! (.connect (defaults :client)))
              coll (.collection (.db client "sample_airbnb") "listingsAndReviews")
              ;;findOne({ name: "Infinite Views" }
              nameOfListing "Infinite Views"
              updatedListing { :bedrooms 6, :beds 8 }
              ;;UPDATE

              _ (println "Before the update")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name nameOfListing)))
              ;;squery-way (update- works only with pipeline update, for not-pipeline update use interop like bellow)
              _ (c-print-all! (update- :sample_airbnb.listingsAndReviews
                                       (uq (=? :name nameOfListing)
                                           (replace-root (merge :ROOT. updatedListing)))))
              _ (println "After the update(beds 8)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name nameOfListing)))
              ;; Drive interop way (2 ways one with pipeline-update and one with update operators)
              updatedListing { :bedrooms 6, :beds 10 }
              _ (.updateOne coll
                            (f (=? :name nameOfListing))    ;;or  (d {:name nameOfListing})
                            (d {"$set" updatedListing}))
              _ (println "After the update2(beds 10)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name nameOfListing)))

              updatedListing {:name "Cozy Cottage", :bedrooms 2, :bathrooms 1}
              _ (println "Before the upsert(see no results)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name "Cozy Cottage")))
              ;;squery-way (update- works only with pipeline update, for not-pipeline update use interop like bellow)
              ;; instead of :sample_airbnb.listingsAndReviews the coll object could be used
              _ (update- :sample_airbnb.listingsAndReviews
                         (uq {:name "Cozy Cottage"} ;;needs a document not a comparison operator, to be the root
                             (replace-root (merge :ROOT. updatedListing))
                             {:upsert true}))
              _ (println "After the upsert(cozy cottage is added)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name "Cozy Cottage")))
              ;;next upsert will be with interop
              updatedListing {:beds 2}
              _ (println "Before second upsert-update(because exists)(see no beds)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name "Cozy Cottage")))
              ;;interop
              _ (.updateOne coll
                            (d {:name "Cozy Cottage"})   ;;or  (d {:name nameOfListing})
                            (d {"$set" updatedListing}))
              _ (println "After the upsert-update(because exists)(after beds=2 is added)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name "Cozy Cottage")))
              ]

          "done")
        (catch :default e (.toString e)))))

(defn aggregate-data []
  (golet [country "Australia"
          market "Sydney"
          maxNumberToPrint 10
          client (<p! (.connect (defaults :client)))
          coll (.collection (.db client "sample_airbnb") "listingsAndReviews")
          _ (c-print-all!
              (q coll
                 (= :bedrooms 1)
                 (= :address.country country)
                 (= :address.market market)
                 (exists? :address.suburb)
                 (not= :address.suburb "")
                 (= :room_type "Entire home/apt")
                 (group {:_id :address.suburb}
                        {:averagePrice (avg :price)})
                 (sort :averagePrice)
                 (limit maxNumberToPrint)))]))


;// DELETE ONE
;        // Check if a listing named "Cozy Cottage" exists. Run update.js if you do not have this listing.
;        await printIfListingExists(client, "Cozy Cottage");
;        // Delete the "Cozy Cottage" listing
;        await deleteListingByName(client, "Cozy Cottage");
;        // Check that the listing named "Cozy Cottage" no longer exists
;        await printIfListingExists(client, "Cozy Cottage");
;
;        // DELETE MANY
;        // Check if the listing named "Ribeira Charming Duplex" (last scraped February 16, 2019) exists
;        await printIfListingExists(client, "Ribeira Charming Duplex");
;        // Check if the listing named "Horto flat with small garden" (last scraped February 11, 2019) exists
;        await printIfListingExists(client, "Horto flat with small garden");
;        // Delete the listings that were scraped before February 15, 2019
;        await deleteListingsScrapedBeforeDate(client, new Date("2019-02-15"));
;        // Check that the listing named "Ribeira Charming Duplex" still exists
;        await printIfListingExists(client, "Ribeira Charming Duplex");
;        // Check that the listing named "Horto flat with small garden" no longer exists
;        await printIfListingExists(client, "Horto flat with small garden");

;; await client.db("sample_airbnb").collection("listingsAndReviews").deleteOne({ name: nameOfListing });

;; await client.db("sample_airbnb").collection("listingsAndReviews").deleteMany({ "last_scraped": { $lt: date } });
(defn delete-data []
  (go (try
        (let [client (<p! (.connect (defaults :client)))
              coll (.collection (.db client "sample_airbnb") "listingsAndReviews")
              ;;findOne({ name: "Infinite Views" }
              nameOfListing "Cozy Cottage"
              ;;DeleteOne

              _ (println "Before the delete(you need to do mongorestore after this to retry it)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name nameOfListing)))
              ;;squery-way (delete- , each dq is like one delete similar to command delete)
              ;; looks like pipeline for simplicity, inside squery makes it a valid delete command (for example limit stage is used)
              ;; one delete can have multiple dq, its like batch delete, like the command
              _ (c-print-all! (delete! :sample_airbnb.listingsAndReviews
                                       (dq (= :name nameOfListing)
                                           (limit 1))))                 ;; for delete many dont put limit
              _ (println "After the delete(expected empty results)")
              _ (c-print-all! (fq :sample_airbnb.listingsAndReviews (=? :name nameOfListing)))
              _ (.exit js/process)
              ;;Delete with driver interop is the same as update with interop, using squery arguments like f,d etc
              ]
          "done")
        (catch :default e (.toString e)))))