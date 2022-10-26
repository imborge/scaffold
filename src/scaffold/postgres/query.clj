(ns scaffold.postgres.query
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [inflections.core :as inflections]
            [scaffold.postgres.constraints :as constraints]
            [scaffold.model :as model])
  (:refer-clojure :exclude [update]))

(s/def ::action #{:insert :select :select-by-pk :delete :update})

(defn hugsql-var [column-spec]
  (clojure.core/update column-spec 0 #(str ":" %)))

(defn hugsql-query-name [table-spec action {:keys [depluralize?]
                                            :or   {depluralize? false}
                                            :as   opts}]
  (let [table-name        (:name table-spec)
        primary-key       (model/find-primary-key-columns table-spec)
        select-by-pk-name (if (model/column-spec? primary-key)
                            (first primary-key)
                            (str/join "-and-" (if (keyword? (first primary-key))
                                                (drop 1 primary-key)
                                                (drop 2 primary-key))))
        query-ns          (if depluralize?
                            (inflections/singular table-name)
                            table-name)]
    (condp = action
      :insert       (str query-ns "/create!")
      :select       (str query-ns "/get")
      :select-by-pk (str query-ns "/get-by-" select-by-pk-name)
      :delete       (str query-ns "/delete!")
      :update       (str query-ns "/update!"))))

(defn hugsql-signature
  ([table-spec action]
   (hugsql-signature table-spec action {}))
  ([table-spec action {:keys [depluralize?]
                       :or   {depluralize? false}
                       :as   opts}]
   {:pre [(s/valid? ::action action)]}
   (let [table-name        (:name table-spec)
         signature         (condp = action
                             :insert       (str (hugsql-query-name table-spec :insert opts) " :! :n")
                             :select       (str (hugsql-query-name table-spec :select opts) " :? :*")
                             :select-by-pk (str (hugsql-query-name table-spec :select-by-pk opts) " :? :1")
                             :delete       (str (hugsql-query-name table-spec :delete opts) " :! :n")
                             :update       (str (hugsql-query-name table-spec :update opts) " :! :n"))]
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

(defn where-and-clause [table-spec prepare-column-value-fn pk-table-constraint]
  (let [columns      (if (constraints/has-name? pk-table-constraint)
                       (drop 2 pk-table-constraint)
                       (drop 1 pk-table-constraint))
        column-specs (map #(model/find-column-by-name % (:columns table-spec)) columns)]
    (str/join " AND " (map #(str (first %) " = " (first (prepare-column-value-fn %))) column-specs))))

(defn where-clause [table-spec prepare-column-value-fn]
  (let [primary-key-columns (model/find-primary-key-columns table-spec)]
    (when (seq primary-key-columns)
      (if (model/column-spec? primary-key-columns)
        (str (first primary-key-columns) " = " (first (prepare-column-value-fn primary-key-columns)))
        (where-and-clause table-spec prepare-column-value-fn primary-key-columns)))))

(defn primary-key-column?
  [table-spec column-name]
  (if-let [table-pk (model/find-primary-key-table-constraint table-spec)]
    (do
      (if (constraints/has-name? table-pk)
        ((set (drop 2 table-pk)) column-name)
        ((set (drop 1 table-pk)) column-name)))
    (model/column-contains-primary-key? (model/find-column-by-name column-name (:columns table-spec)))))

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
