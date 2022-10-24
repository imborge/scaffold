(ns scaffold.request-handler-test
  (:require [scaffold.request-handler :as sut]
            [clojure.test :as t]))

(def table-1-spec
  {:name    "users"
   :columns [["id" [:uuid] [[:primary-key]]]
             ["username" [:text]]]})

#_(deftest create-handler
  (testing "can create `create` handler"
    (sut/create table-1-spec)))
