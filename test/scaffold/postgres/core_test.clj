(ns scaffold.postgres.core-test
  (:require [scaffold.postgres.core :as sut]
            [clojure.test :refer :all]))

(deftest table
  (testing "can create table"
    (is (= "CREATE TABLE users (\n);" (sut/table-sql {:name "users"}))))
  
  (testing "can create table with column"
    (is (= (str "CREATE TABLE users (\nid UUID);")
           (sut/table-sql {:name    "users"
                                    :columns [["id" [:uuid] []]]}))))

  (testing "can create table with two columns"
    (is (= (str "CREATE TABLE users (\n"
                "id UUID,\n"
                "username TEXT);")
           (sut/table-sql {:name    "users"
                                    :columns [["id" [:uuid] []]
                                              ["username" [:text] []]]})))))
