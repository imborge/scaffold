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

(defn default? [constraint]
  (= :default (first constraint)))

(def referential-action
  #{:restrict :cascade :noop :null :default})

(s/def ::referential-rule
  (s/cat
   :rule #{:on-delete :on-update}
   :action referential-action))

(s/def ::referential-action referential-action)
(s/def ::references
  (s/cat
   :table string?
   :column string?
   :rules (s/* ::referential-rule)))

(def referential-action->str
  {:restrict "RESTRICT"
   :cascade  "CASCADE"
   :noop     "NO ACTION"
   :null     "SET NULL"
   :default  "SET DEFAULT"})

(defn rule-count [rules]
  (frequencies (map :rule rules)))

(defn validate-unique-rules [rules]
  (let [rc (rule-count rules)]
    (when (some #(<= 2 %) (vals rc))
      (throw (ex-info (str "Validation error! Multiple rules provided for: " (keys (filter #(< 1 (val %)) rc))) {:rule-count rc})))
    (every? #(<= % 1) (vals (rule-count rules)))))

(defn references [[table column & {:keys [on-delete on-update]} :as params]]
  {:pre [(s/valid? ::references (vec params))]}
  (let [conformed (s/conform ::references (vec params))]
    (if (and (not (s/invalid? conformed))
             (validate-unique-rules (:rules conformed)))
      (str "REFERENCES " table "(" column ")"
           (when on-delete
             (str " ON DELETE " (referential-action->str on-delete)))
           (when on-update
             (str " ON UPDATE " (referential-action->str on-update))))
      (throw (ex-info "")))))

(def column-constraint->generator
  {:not-null    (constantly "NOT NULL")
   :null        (constantly "NULL")
   :unique      (constantly "UNIQUE")
   :check       check
   :default     default
   :primary-key (constantly "PRIMARY KEY")
   :foreign-key references
   :references  references})

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

(defn table-constraint? [constraint-vec]
  (keyword? (first constraint-vec)))

(defn column-names-from-constraint [constraint-vec]
  (if (table-constraint? constraint-vec)
    (vec (drop 1 constraint-vec))
    [(first constraint-vec)]))
