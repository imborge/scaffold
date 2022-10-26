(ns scaffold.model
  (:require [scaffold.postgres.constraints :as constraints]))

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

(defn column-has-default?
  [[column-name column-type constraints :as column-spec]]
  (some true? (map constraints/default? constraints)))

(defn find-primary-key-column [column-specs]
  (first (filter column-contains-primary-key? column-specs)))

(defn find-primary-key-columns [table-spec]
  (or (find-primary-key-table-constraint table-spec)
      (find-primary-key-column (:columns table-spec))))

(defn find-column-by-name [column-name column-specs]
  (first (filter #(= column-name (first %)) column-specs)))
