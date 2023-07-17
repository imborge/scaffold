(ns scaffold.request-handlers-test
  (:require [scaffold.request-handlers :as sut]
            [clojure.test :refer :all]
            [ring.util.http-response :as response]
            [clojure.string :as str]))

(defn make-query-fn [ret-val]
  (list 'fn '[query params]
        ret-val))

(deftest handlers
  (binding [*ns* (find-ns 'scaffold.request-handlers-test)]
    (testing "create handler returns 201 when query yields > 0 (rows inserted)"
      (let [create
            (eval (sut/create (make-query-fn 1) :noquery :defn? false))]
        (is (= 201 (:status (create {}))))))
    (testing "create handler returns 400 when query yields <= 0 (rows inserted)"
      (let [create (eval (sut/create (make-query-fn 0) :noquery :defn? false))]
        (is (= 400 (:status (create {}))))))
    (testing "index handler"
      (let [index (eval (sut/index (make-query-fn {}) :noquery :defn? false))]
        (is (= 200 (:status (index {}))))))
    (testing "detail handler returns 200 when query yields an entity"
      (let [single (eval (sut/single (make-query-fn {:id 1 :username "test"}) :noquery :defn? false))]
        (is (= 200 (:status (single {}))))))
    (testing "single handler returns 404 when query yields nil"
      (let [single (eval (sut/single (make-query-fn nil) :noquery :defn? false))]
        (is (= 404 (:status (single {}))))))
    (testing "update handler returns 200 on success"
      (let [update (eval (sut/update (make-query-fn {:id 2 :username "test"}) :noquery :defn? false))]
        (is (= 200 (:status (update {}))))))
    (testing "update handler with entity not existing returns 400"
      (let [update (eval (sut/update (make-query-fn nil) :noquery :defn? false))]
        (is (= 400 (:status (update {}))))))
    (testing "delete handler with query returning > 0 returns 204"
      (let [delete (eval (sut/delete (make-query-fn 1) :noquery :defn? false))]
        (is (= 204 (:status (delete {}))))))
    (testing "delete handler with query returning <= 0 returns 400"
      (let [delete (eval (sut/delete (make-query-fn 0) :noquery :defn? false))]
        (is (= 400 (:status (delete {}))))))))
