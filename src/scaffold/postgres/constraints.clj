(ns scaffold.postgres.constraints
  (:require [clojure.string :as str]))

(defn has-name? [constraint]
  (string? (first constraint)))

(defn generate-unique-constraint [constraint]
  (str "UNIQUE"))

(defn generate-check-constraint [[constraint]]
  (str "CHECK (" constraint ")"))

(defn generate-default-constraint [[constraint]]
  (str "DEFAULT " constraint))

(defn generate-primary-key-constraint [[constraint]]
  (str "PRIMARY KEY"))

(defn generate-references-constraint [[constraint]]
  (str "REFERENCES " constraint))

(defn generate-column-vector-constraint [constraint]
  (let [constraint-name          (when (has-name? constraint)
                                   (first constraint))
        [constraint-type & rest] (if (has-name? constraint)
                                   (rest constraint)
                                   constraint)]
    (str (when constraint-name
           (str "CONSTRAINT " constraint-name " "))
         (condp = constraint-type
           :unique (generate-unique-constraint rest)
           :check  (generate-check-constraint rest)
           :default (generate-default-constraint rest)
           :primary-key (generate-primary-key-constraint rest)
           :references (generate-references-constraint rest)))))

(defn generate-column-constraint [constraint]
  (if (vector? constraint)
    (generate-column-vector-constraint constraint)
    (condp = constraint
      :not-null    "NOT NULL"
      :null        "NULL"
      :unique      "UNIQUE"
      :primary-key "PRIMARY KEY")))

(defn generate-column-constraints [constraints]
  (str/join " " (map generate-column-constraint constraints)))
