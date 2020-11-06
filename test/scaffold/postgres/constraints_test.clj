(ns scaffold.postgres.constraints-test
  (:require [scaffold.postgres.constraints :as sut]
            [clojure.test :refer :all]))

(deftest simple-column-constraints
  (testing "NOT NULL"
    (is (= "NOT NULL" (sut/generate-column-constraint [:not-null]))))
  (testing "NULL"
    (is (= "NULL" (sut/generate-column-constraint [:null]))))
  (testing "PRIMARY KEY"
    (is (= "PRIMARY KEY" (sut/generate-column-constraint [:primary-key]))))
  (testing "UNIQUE"
    (is (= "UNIQUE" (sut/generate-column-constraint [:unique])))))

(deftest advanced-column-constraints
  (testing "can create constraint with a name"
    (is (= "CONSTRAINT unique_name UNIQUE"
           (sut/generate-column-constraint ["unique_name" :unique]))))
  (testing "can create check constraint"
    (is (= "CHECK (LEN(name) > 13)"
           (sut/generate-column-constraint [:check "LEN(name) > 13"]))))
  (testing "can create default constraint"
    (is (= "DEFAULT uuid_generate_v4()"
           (sut/generate-column-constraint [:default "uuid_generate_v4()"]))))
  (testing "can create primary-key constraint"
    (is (= "PRIMARY KEY"
           (sut/generate-column-constraint [:primary-key]))))
  (testing "can create references constraint"
    (is (= "REFERENCES users(id)"
           (sut/generate-column-constraint [:references "users" "id"])))))

(deftest multi-constraints
  (testing "no constraints"
    (is (= ""
           (sut/generate-column-constraints []))))
  (testing "can create multiple constraints"
    (is (= "NOT NULL REFERENCES users(id)"
           (sut/generate-column-constraints [[:not-null] [:references "users" "id"]])))))
