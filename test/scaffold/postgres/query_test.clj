(ns scaffold.postgres.query-test
  (:require [scaffold.postgres.query :as sut]
            [clojure.test :refer :all]))

(deftest insert-queries
  (testing "can create insert query"
    (is (= "INSERT INTO users (id)\nVALUES (:id::UUID)"
           (sut/insert {:name    "users"
                        :columns [["id" [:uuid]]]}
                       (comp sut/hugsql-var sut/append-column-cast))))))

(deftest select-queries
  (testing "can create select query"
    (is (= "SELECT id FROM users"
           (sut/select {:name "users"
                                 :columns [["id" [:uuid]]]})))))

(deftest update-queries
  (testing "can create update query"
    (is (= "UPDATE users SET\nusername = :username\nWHERE id = :id::UUID"
           (sut/update {:name             "users"
                        :columns [["id" [:uuid] [[:primary-key]]]
                                  ["username" [:text]]]}
                       (comp sut/hugsql-var sut/append-column-cast))))
    (is (= "UPDATE users SET\nusername = :username,\npassword = :password\nWHERE id = :id::UUID AND email = :email"
           (sut/update {:name                 "users"
                        :columns     [["id" [:uuid]]
                                      ["username" [:text]]
                                      ["email" [:text]]
                                      ["password" [:text]]]
                        :constraints [[:primary-key "id" "email"]]}
                       (comp sut/hugsql-var sut/append-column-cast))))))

(deftest delete-queries
  (testing "can create delete query"
    (is (= "DELETE FROM users WHERE id = :id::UUID"
           (sut/delete {:name             "users"
                        :columns [["id" [:uuid] [[:primary-key]]]]}
                       (comp sut/hugsql-var sut/append-column-cast))))))
