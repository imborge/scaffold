(ns scaffold.postgres.constraints
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(defn has-name?
  "The constraint has a name is the first element is a string"
  [constraint]
  (string? (first constraint)))

(defn primary-key?
  "Checks if a constraint spec is a :primary-key"
  [constraint-spec]
  (or (= :primary-key (first constraint-spec))
      (= :primary-key (second constraint-spec))))

(defn check [[expr]]
  {:pre [(s/valid? string? expr)]}
  (str "CHECK (" expr ")"))

(defn default [[expr]]
  {:pre [(s/valid? string? expr)]}
  (str "DEFAULT " expr))

(def referential-action
  #{:restrict :cascade :noop :null :default})

(s/def ::referential-action referential-action)
(s/def ::references
  (s/cat
   :table string?
   :column string?
   :on-delete (s/? (s/nilable ::referential-action))
   :on-update (s/? (s/nilable ::referential-action))))

(def referential-action->str
  {:restrict "RESTRICT"
   :cascade  "CASCADE"
   :noop     "NO ACTION"
   :null     "SET NULL"
   :default  "SET DEFAULT"})

(defn references [[table column on-delete on-update :as params]]
  {:pre [(s/valid? ::references (vec params))]}
  (str "REFERENCES " table "(" column ")"
       (when on-delete
         (str " ON DELETE " (referential-action->str on-delete)))
       (when on-update
         (str " ON UPDATE " (referential-action->str on-update)))))

(def column-constraint->generator
  {:not-null    (constantly "NOT NULL")
   :null        (constantly "NULL")
   :unique      (constantly "UNIQUE")
   :check       check
   :default     default
   :primary-key (constantly "PRIMARY KEY")
   :foreign-key  references})

(defn constraint
  ([constraint-vec constraint->generator-map]
   (let [constraint-name       (when (has-name? constraint-vec)
                                 (first constraint-vec))
         [constraint & params] (if constraint-name
                                 (rest constraint-vec)
                                 constraint-vec)
         generator-fn          (constraint->generator-map constraint)]
     (str (when constraint-name (str "CONSTRAINT " constraint-name " "))
          (generator-fn params)))))

(defn column-constraint [constraint-vec]
  (constraint constraint-vec column-constraint->generator))

(defn column-constraints [constraints]
  (str/join " " (map column-constraint constraints)))

(defn table-unique [columns]
  (str "UNIQUE ("
       (str/join ", " columns)
       ")"))

(defn table-primary-key [columns]
  (str "PRIMARY KEY ("
       (str/join ", " columns)
       ")"))

(s/def ::table-foreign-key
  (s/cat
   :ref-table string?
   :col-pairs (s/coll-of (s/tuple string? string?))
   :on-delete (s/? (s/nilable ::referential-action))
   :on-update (s/? (s/nilable ::referential-action))))

(defn table-foreign-key [params]
  {:pre [(s/valid? ::table-foreign-key params)]}
  (let [parsed-params (s/conform ::table-foreign-key params)]
    (str "FOREIGN KEY (" (str/join ", " (map first (:col-pairs parsed-params))) ") "
         "REFERENCES " (:ref-table parsed-params) " (" (str/join ", " (map second (:col-pairs parsed-params))) ")"
         (when-let [on-delete (:on-delete parsed-params)]
           (str " ON DELETE " (referential-action->str on-delete)))
         (when-let [on-update (:on-update parsed-params)]
           (str " ON UPDATE " (referential-action->str on-update))))))

(def table-constraint->generator
  {:check       check
   :unique      table-unique
   :primary-key table-primary-key
   :foreign-key table-foreign-key})

(defn table-constraint [constraint-vec]
  (constraint constraint-vec table-constraint->generator))
