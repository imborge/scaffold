(ns scaffold.postgres.query
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [inflections.core :as inflections]
            [scaffold.postgres.constraints :as constraints])
  (:refer-clojure :exclude [update] ))

(s/def ::action #{:insert :select :select-by-pk :delete :update})

(defn table-spec->query-names [table-spec]
  (map
   (fn [action]
     [action (keyword (name (:name table-spec)) (name action))])
   (s/describe ::action)))

(defn column-spec? [table-constraint-or-column]
  ;; A column is a vector where the first item is the column-name
  ;; second item is a vector containing the type
  ;; third item is a vector of constraints
  (and (string? (nth table-constraint-or-column 0))
       (vector? (nth table-constraint-or-column 1))
       (vector? (nth table-constraint-or-column 2))))

(defn find-primary-key-table-constraint
  "Finds the constraint in the table-spec :constraints which is a :primary-key"
  [table-spec]
  (first (filter constraints/primary-key? (:constraints table-spec))))

(defn column-contains-primary-key?
  "Checks if the column in the column-spec has a :primary-key constraint"
  [[_ _ constraints
    :as column-spec]]
  (seq (filter constraints/primary-key? constraints)))

(defn find-primary-key-column [column-specs]
  (first (filter column-contains-primary-key? column-specs)))

(defn find-primary-key-columns [table-spec]
  (or (find-primary-key-table-constraint table-spec)
      (find-primary-key-column (:columns table-spec))))

(defn hugsql-var [column-spec]
  (clojure.core/update column-spec 0 #(str ":" %)))

(defn hugsql-query-name [table-spec action {:keys [depluralize?]
                                            :or   {depluralize? false}
                                            :as   opts}]
  (let [table-name        (:name table-spec)
        primary-key       (find-primary-key-columns table-spec)
        select-by-pk-name (if (column-spec? primary-key)
                            (first primary-key)
                            (str/join "-and-" (if (keyword? (first primary-key))
                                                (drop 1 primary-key)
                                                (drop 2 primary-key))))
        query-ns          (if depluralize?
                            (inflections/singular table-name)
                            table-name)]
    (condp = action
      :insert       (str query-ns "/create")
      :select       (str query-ns "/get")
      :select-by-pk (str query-ns "/get-by-" select-by-pk-name)
      :delete       (str query-ns "/delete")
      :update       (str query-ns "/update"))))

(defn hugsql-signature
  ([table-spec action]
   (hugsql-signature table-spec action {}))
  ([table-spec action {:keys [depluralize?]
                       :or   {depluralize? false}
                       :as   opts}]
   {:pre [(s/valid? ::action action)]}
   (let [table-name        (:name table-spec)
         signature         (condp = action
                             :insert       (str (hugsql-query-name table-spec :insert opts) "! :! :n")
                             :select       (str (hugsql-query-name table-spec :select opts) " :? :*")
                             :select-by-pk (str (hugsql-query-name table-spec :select-by-pk opts) " :? :1")
                             :delete       (str (hugsql-query-name table-spec :delete opts) "! :! :n")
                             :update       (str (hugsql-query-name table-spec :update opts) "! :! :n"))]
     (str "-- :name " signature "\n" ))))

(defn jdbc-val [column-spec]
  (clojure.core/update column-spec 0 (constantly "?")))

(defn append-column-cast [[_ [column-type] :as column-spec]]
  (let [append-str (condp = column-type
                     :uuid        "::UUID"
                     :inet        "::INET"
                     :time        "::TIME"
                     :timetz      "::TIMETZ"
                     :timestamp   "::TIMESTAMP"
                     :timestamptz "::TIMESTAMPTZ"
                     nil)]
    (clojure.core/update column-spec 0 #(str % append-str))))

(defn update-field [column-spec prepare-column-value-fn]
  (let [[column-name] column-spec]
    (str column-name " = " (first (prepare-column-value-fn column-spec)))))

(defn update-fields [column-specs prepare-column-value-fn]
  (mapv #(update-field % prepare-column-value-fn) column-specs))

(defn find-column-by-name [column-name column-specs]
  (first (filter #(= column-name (first %)) column-specs)))

(defn where-and-clause [table-spec prepare-column-value-fn pk-table-constraint]
  (let [columns      (if (constraints/has-name? pk-table-constraint)
                       (drop 2 pk-table-constraint)
                       (drop 1 pk-table-constraint))
        column-specs (map #(find-column-by-name % (:columns table-spec)) columns)]
    (str/join " AND " (map #(str (first %) " = " (first (prepare-column-value-fn %))) column-specs))))

(defn where-clause [table-spec prepare-column-value-fn]
  (let [primary-key-columns (find-primary-key-columns table-spec)]
    (when (seq primary-key-columns)
      (if (column-spec? primary-key-columns)
        (str (first primary-key-columns) " = " (first (prepare-column-value-fn primary-key-columns)))
        (where-and-clause table-spec prepare-column-value-fn primary-key-columns)))))

(defn primary-key-column?
  [table-spec column-name]
  (if-let [table-pk (find-primary-key-table-constraint table-spec)]
    (do
      (if (constraints/has-name? table-pk)
        ((set (drop 2 table-pk)) column-name)
        ((set (drop 1 table-pk)) column-name)))
    (column-contains-primary-key? (find-column-by-name column-name (:columns table-spec)))))

(defn insert [{table-name :name
               columns :columns}
              prepare-column-value-fn]
  (str "INSERT INTO " table-name
       " (" (str/join ", " (map first columns)) ")\n"
       "VALUES (" (str/join ", " (map (comp first prepare-column-value-fn) columns)) ")"))

(defn select [{table-name :name
               columns    :columns
               :as        table-spec}]  
  (str "SELECT " (str/join ", " (map first columns)) " FROM " table-name))

(defn select-by-pk [{table-name :name
                     columns          :columns
                     :as              table-spec}
                    prepare-column-value-fn]  
  (str "SELECT " (str/join ", " (map first columns)) " FROM " table-name
       " WHERE " (where-clause table-spec prepare-column-value-fn)))

(defn update [{table-name :name
                        columns    :columns
                        :as        table-spec}
                       prepare-column-value-fn]
  (str "UPDATE " table-name " SET"
       "\n"
       (str/join ",\n" (update-fields (remove #(primary-key-column? table-spec (first %)) (:columns table-spec)) prepare-column-value-fn))
       "\n"
       "WHERE " (where-clause table-spec prepare-column-value-fn)))

(defn delete [{table-name :name
                        columns    :columns
                        :as        table-spec}
                       prepare-column-value-fn]
  (str "DELETE FROM " table-name " WHERE " (where-clause table-spec prepare-column-value-fn)))
