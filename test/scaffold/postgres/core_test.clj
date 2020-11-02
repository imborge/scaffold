(ns scaffold.postgres.core-test
  (:require [scaffold.postgres.core :as sut]
            [clojure.test :refer :all]))

(deftest types
  (testing "can generate column named \"description\" with type \"text\" with check constraint\""
    (is (= "description TEXT CHECK (description = 'lol')"
           (sut/generate-column-sql ["description" :text [[:check "description = 'lol'"]]])))))

(deftest table
  (testing "can create table"
    (is (= "CREATE TABLE users (\n);" (sut/generate-table-sql {:name "users"}))))
  
  (testing "can create table with column"
    (is (= (str "CREATE TABLE users (\nid UUID);")
           (sut/generate-table-sql {:name    "users"
                                    :columns [["id" :uuid []]]}))))

  (testing "can create table with two columns"
    (is (= (str "CREATE TABLE users (\n"
                "id UUID,\n"
                "username TEXT);")
           (sut/generate-table-sql {:name    "users"
                                    :columns [["id" :uuid []]
                                              ["username" :text []]]})))))
