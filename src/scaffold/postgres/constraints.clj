(ns scaffold.postgres.constraints
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(defn has-name?
  "The constraint has a name is the first element is a string"
  [constraint]
  (string? (first constraint)))

(defn generate-check [[expr]]
  {:pre [(s/valid? string? expr)]}
  (str "CHECK (" expr ")"))

(defn generate-default [[expr]]
  {:pre [(s/valid? string? expr)]}
  (str "DEFAULT " expr))

(s/def ::on-delete #{:restrict :cascade :noop :null :default})
(s/def ::references (s/cat :table string? :column string? :on-delete (s/? ::on-delete)))

(def on-delete->str
  {:restrict "RESTRICT"
   :cascade  "CASCADE"
   :noop     "NO ACTION"
   :null     "SET NULL"
   :default  "SET DEFAULT"})

(defn generate-references [[table column on-delete :as params]]
  {:pre [(s/valid? ::references (vec params))]}
  (str "REFERENCES " table "(" column ")"
       (when on-delete
         (str " " (on-delete->str on-delete)))))

(def column-constraint->generator
  {:not-null    (constantly "NOT NULL")
   :null        (constantly "NULL")
   :unique      (constantly "UNIQUE")
   :check       generate-check
   :default     generate-default
   :primary-key (constantly "PRIMARY KEY")
   :references  generate-references})

(defn generate-column-constraint
  ([constraint]
   (generate-column-constraint constraint column-constraint->generator))
  ([constraint column-constraint->generator-map]
   (let [constraint-name       (when (has-name? constraint)
                                 (first constraint))
         [constraint & params] (if constraint-name
                                 (rest constraint)
                                 constraint)
         generator-fn          (column-constraint->generator-map constraint)]
     (str (when constraint-name (str "CONSTRAINT " constraint-name " "))
          (generator-fn params)))))

(defn generate-column-constraints [constraints]
  (str/join " " (map generate-column-constraint constraints)))
