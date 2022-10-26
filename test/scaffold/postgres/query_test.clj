(ns scaffold.postgres.query-test
  (:require [scaffold.postgres.query :as sut]
            [clojure.test :refer :all]))

(deftest insert-queries
  (testing "can create insert query"
    (is (= "INSERT INTO users (id)\nVALUES (:id::UUID)"
           (sut/insert {:name    "users"
                        :columns [["id" [:uuid]]]}
                       (comp sut/hugsql-var sut/append-column-cast)))))

  (testing "can create insert query without values for default fields"
    (is (= "INSERT INTO users (email, password)\nVALUES (:email, :password)"
           (sut/insert {:name "users"
                        :columns [["id" [:uuid] [[:default "uuid_generate_v4()"]]]
                                  ["email" [:text]]
                                  ["password" [:text]]]}
                       (comp sut/hugsql-var sut/append-column-cast))))))

(deftest select-queries
  (testing "can create select query"
    (is (= "SELECT id FROM users"
           (sut/select {:name "users"
                        :columns [["id" [:uuid]]]})))))

(deftest select-by-pk-queries
  (testing "can create select-by-{pk} query"
    (is (= "SELECT id FROM users WHERE id = :id::UUID"
           (sut/select-by-pk {:name    "users"
                              :columns [["id" [:uuid] [[:primary-key]]]]}
                             (comp sut/hugsql-var sut/append-column-cast)))))
  (testing "can create select-by-{pk} with composite primary key"
    (is (= "SELECT id, username FROM users WHERE id = :id::UUID AND username = :username"
           (sut/select-by-pk {:name    "users"
                              :columns [["id" [:uuid]]
                                        ["username" [:text]]]
                              :constraints [[:primary-key "id" "username"]]}
                             (comp sut/hugsql-var sut/append-column-cast))))))

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

(deftest hugsql
  (let [table-1-spec
        {:name    "users"
         :columns [["id" [:uuid] [[:primary-key]]]
                   ["username" [:text]]]}

        table-2-spec
        {:name    "users"
         :columns [["id" [:uuid]]
                   ["username" [:text]]]
         :constraints [[:primary-key "id" "username"]]}]
    (testing "can create insert signature"
      (is (= "-- :name user/create! :! :n\n"
             (sut/hugsql-signature table-1-spec :insert {:depluralize? true}))))
    (testing "can create select signature"
      (is (= "-- :name user/get :? :*\n"
             (sut/hugsql-signature table-1-spec :select {:depluralize? true}))))
    (testing "can create select-by-{pk} signature"
      (is (= "-- :name user/get-by-id :? :1\n"
             (sut/hugsql-signature table-1-spec :select-by-pk {:depluralize? true}))))
    (testing "can create select-by-{pk}s signature"
      (is (= "-- :name user/get-by-id-and-username :? :1\n"
             (sut/hugsql-signature table-2-spec :select-by-pk {:depluralize? true}))))
    (testing "can create update signature"
      (is (= "-- :name user/update! :! :n\n"
             (sut/hugsql-signature table-1-spec :update {:depluralize? true}))))
    (testing "can create delete signature"
      (is (= "-- :name user/delete! :! :n\n"
             (sut/hugsql-signature table-1-spec :delete {:depluralize? true}))))))
