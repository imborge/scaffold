(ns scaffold.postgres.types-test
  (:require [scaffold.postgres.types :as sut]
            [clojure.test :refer :all]))

(deftest types  
  (testing "can create BIT"
    (is (= "BIT"
           (sut/generate-type [:bit]))))
  (testing "can create BIT(10)"
    (is (= "BIT(10)"
           (sut/generate-type [:bit 10]))))

  (testing "can create VARBIT"
    (is (= "VARBIT"
           (sut/generate-type [:varbit]))))
  (testing "can create VARBIT(10)"
    (is (= "VARBIT(10)"
           (sut/generate-type [:varbit 10]))))
  
  (testing "can create CHAR"
    (is (= "CHAR"
           (sut/generate-type [:char]))))
  (testing "can create CHAR(10)"
    (is (= "CHAR(10)"
           (sut/generate-type [:char 10]))))
  
  (testing "can create VARCHAR"
    (is (= "VARCHAR"
           (sut/generate-type [:varchar]))))
  (testing "can create VARCHAR(255)"
    (is (= "VARCHAR(255)"
           (sut/generate-type [:varchar 255]))))

  (testing "can create NUMERIC"
    (is (= "NUMERIC"
           (sut/generate-type [:numeric]))))
  (testing "can create NUMERIC(10)"
    (is (= "NUMERIC(10)"
           (sut/generate-type [:numeric 10]))))
  (testing "can create NUMERIC(10,2)"
    (is (= "NUMERIC(10,2)"
           (sut/generate-type [:numeric 10 2]))))

  (testing "can create TIME"
    (is (= "TIME"
           (sut/generate-type [:time]))))
  (testing "can create TIME(6)"
    (is (= "TIME(6)"
           (sut/generate-type [:time 6]))))

  (testing "can create TIME WITH TIME ZONE"
    (is (= "TIME WITH TIME ZONE"
           (sut/generate-type [:timetz]))))
  (testing "can create TIME(6) WITH TIME ZONE"
    (is (= "TIME(6) WITH TIME ZONE"
           (sut/generate-type [:timetz 6]))))

  (testing "can create TIMESTAMP)"
    (is (= "TIMESTAMP"
           (sut/generate-type [:timestamp]))))
  (testing "can create TIMESTAMP(6)"
    (is (= "TIMESTAMP(6)"
           (sut/generate-type [:timestamp 6]))))

  (testing "can create TIMESTAMP WITH TIME ZONE"
    (is (= "TIMESTAMP WITH TIME ZONE"
           (sut/generate-type [:timestamptz]))))
  (testing "can create TIMESTAMP(6) WITH TIME ZONE"
    (is (= "TIMESTAMP(6) WITH TIME ZONE"
           (sut/generate-type [:timestamptz 6])))))

