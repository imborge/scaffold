(ns scaffold.postgres.constraints-test
  (:require [scaffold.postgres.constraints :as sut]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(deftest simple-column-constraints
  (testing "NOT NULL"
    (is (= "NOT NULL" (sut/column-constraint [:not-null]))))
  (testing "NULL"
    (is (= "NULL" (sut/column-constraint [:null]))))
  (testing "PRIMARY KEY"
    (is (= "PRIMARY KEY" (sut/column-constraint [:primary-key]))))
  (testing "UNIQUE"
    (is (= "UNIQUE" (sut/column-constraint [:unique])))))

(deftest advanced-column-constraints
  (testing "can create constraint with a name"
    (is (= "CONSTRAINT unique_name UNIQUE"
           (sut/column-constraint ["unique_name" :unique]))))
  (testing "can create check constraint"
    (is (= "CHECK (LEN(name) > 13)"
           (sut/column-constraint [:check "LEN(name) > 13"]))))
  (testing "can create default constraint"
    (is (= "DEFAULT uuid_generate_v4()"
           (sut/column-constraint [:default "uuid_generate_v4()"]))))
  (testing "can create primary-key constraint"
    (is (= "PRIMARY KEY"
           (sut/column-constraint [:primary-key]))))
  (testing "can create references constraint"
    (is (= "REFERENCES users(id)"
           (sut/column-constraint [:foreign-key "users" "id"]))))
  (testing "can create references constraint with on delete"
    (doseq [action sut/referential-action]
      (is (= (str "REFERENCES users(id) ON DELETE " (sut/referential-action->str action))
             (sut/column-constraint [:foreign-key "users" "id" action])))))
  (testing "can create references constraint with on update"
    (doseq [action sut/referential-action]
      (is (= (str "REFERENCES users(id) ON UPDATE " (sut/referential-action->str action))
             (sut/column-constraint [:foreign-key "users" "id" nil action])))))
  (testing "can create references constraint with on delete and on update"
    (doseq [delete-action sut/referential-action
            update-action sut/referential-action]
      (is (= (str "REFERENCES users(id) ON DELETE "
                  (sut/referential-action->str delete-action)
                  " ON UPDATE " (sut/referential-action->str update-action))
             (sut/column-constraint [:foreign-key "users" "id" delete-action update-action]))))))


(deftest multi-constraints
  (testing "no constraints"
    (is (= ""
           (sut/column-constraints []))))
  (testing "can create multiple constraints"
    (is (= "NOT NULL REFERENCES users(id)"
           (sut/column-constraints [[:not-null] [:foreign-key "users" "id"]])))))

(deftest table-constraints
  (testing "can generate CHECK constraint"
    (is (= "CHECK (1 = 1)"
           (sut/table-constraint [:check "1 = 1"]))))
  (testing "can generate UNIQUE constraint"
    (is (= "UNIQUE (id)"
           (sut/table-constraint [:unique "id"])))
    (is (= "UNIQUE (from, to)"
           (sut/table-constraint [:unique "from" "to"]))))
  (testing "can generate PRIMARY KEY constraint"
    (is (= "PRIMARY KEY (id)"
           (sut/table-constraint [:primary-key "id"])))
    (is (= "PRIMARY KEY (name, number)"
           (sut/table-constraint [:primary-key "name" "number"]))))
  (testing "can generate FOREIGN KEY constraint"
    (is (= "FOREIGN KEY (author_id) REFERENCES authors (id)"
           (sut/table-constraint [:foreign-key "authors" [["author_id" "id"]]])))
    (is (= "FOREIGN KEY (author_id, something) REFERENCES authors (id, something)"
           (sut/table-constraint [:foreign-key "authors" [["author_id" "id"] ["something" "something"]]]))))
  (testing "can generate FOREIGN KEY constraint with ON DELETE"
    (doseq [action sut/referential-action]
      (is (= (str "FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE " (sut/referential-action->str action))
             (sut/table-constraint [:foreign-key "authors" [["author_id" "id"]] action])))))
  (testing "can generate FOREIGN KEY constraint with ON UPDATE"
    (doseq [action sut/referential-action]
      (is (= (str "FOREIGN KEY (author_id) REFERENCES authors (id) ON UPDATE " (sut/referential-action->str action))
             (sut/table-constraint [:foreign-key "authors" [["author_id" "id"]] nil action])))))
  (testing "can generate FOREIGN KEY constraint with ON DELETE and ON UPDATE"
    (doseq [delete-action sut/referential-action
            update-action sut/referential-action]
      (is (= (str "FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE "
                  (sut/referential-action->str delete-action)
                  " ON UPDATE "
                  (sut/referential-action->str update-action))
             (sut/table-constraint [:foreign-key "authors" [["author_id" "id"]] delete-action update-action]))))))
