(ns scaffold.postgres.query-test
  (:require [scaffold.postgres.query :as sut]
            [clojure.test :refer :all]))

(deftest insert-queries
  (testing "can create insert query"
    (is (= "INSERT INTO users (id)\nVALUES (:id::UUID)"
           (sut/generate-insert {:name    "users"
                                 :columns [["id" [:uuid]]]}
                                (comp sut/prepare-hugsql-val sut/append-column-cast))))))

(deftest select-queries
  (testing "can create select query"
    (is (= "SELECT id FROM users"
           (sut/generate-select {:name "users"
                                 :columns [["id" [:uuid]]]})))))

(deftest update-queries
  (testing "can create update query"
    (is (= "UPDATE users SET\nusername = :username\nWHERE id = :id::UUID"
           (sut/generate-update {:name    "users"
                                 :columns [["id" [:uuid] [[:primary-key]]]
                                           ["username" [:text]]]}
                                (comp sut/prepare-hugsql-val sut/append-column-cast))))
    (is (= "UPDATE users SET\nusername = :username,\npassword = :password\nWHERE id = :id::UUID AND email = :email"
           (sut/generate-update {:name    "users"
                                 :columns [["id" [:uuid]]
                                           ["username" [:text]]
                                           ["email" [:text]]
                                           ["password" [:text]]]
                                 :constraints [[:primary-key "id" "email"]]}
                                (comp sut/prepare-hugsql-val sut/append-column-cast))))))

(deftest delete-queries
  (testing "can create delete query"
    (is (= "DELETE FROM users WHERE id = :id::UUID"
           (sut/generate-delete {:name "users"
                                 :columns [["id" [:uuid] [[:primary-key]]]]}
                                (comp sut/prepare-hugsql-val sut/append-column-cast))))))
