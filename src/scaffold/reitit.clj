(ns scaffold.reitit
  (:require [scaffold.postgres.constraints :as c]
            [scaffold.postgres.query :as q]
            [clojure.string :as str]
            [scaffold.postgres.types :as t]
            [scaffold.model :as model]
            [scaffold.request-handler :as handlers]))

(defn primary-key-path
  "Generates the reitit resource path for `detail` and `delete`.

  The path is generated from the primary key columns, e.g.
  Given a table-spec with `id` as the primary key returns
  \"/:id\"

  Given a table-spec with `id` and `email` as primary keys returns
  \"/:id/:email\""
  [table-spec]
  (let [primary-keys (model/find-primary-key-columns table-spec)]
    (str "/" (str/join "/" (map keyword (c/column-names-from-constraint primary-keys))))))

(defn primary-key-parameters [table-spec]
  (let [primary-keys (model/find-primary-key-columns table-spec)
        column-names (c/column-names-from-constraint primary-keys)]
    (into
     {}
     (map
      (fn [column-name]
        (let [[_ [column-type] _] (model/find-column-by-name column-name (:columns table-spec))]
          [(keyword column-name)
           (t/pgtype->clj-predicate column-type)]))
      column-names))))

(defn body-parameters [table-spec]
  (let [primary-key-column-names (-> table-spec
                                     model/find-primary-key-columns
                                     c/column-names-from-constraint)]
    (into
     {}
     (map
      (fn [column-name]
        (let [[_ [column-type] _] (model/find-column-by-name column-name (:columns table-spec))]
          [(keyword column-name)
           (t/pgtype->clj-predicate column-type)]))
      (->> (:columns table-spec)
           (map first) ;; column name
           (remove (set primary-key-column-names)))))))

(defn routes [table-spec query-fn]
  [(str "/" (:name table-spec))
   ["/"
    {:get {:handler (handlers/index query-fn (keyword (q/hugsql-query-name table-spec :select {:depluralize? true})))}
     :post {:parameters {:body (body-parameters table-spec)}
            :handler (handlers/create query-fn (keyword (q/hugsql-query-name table-spec :insert {:depluralize? true})))}}]
   [(primary-key-path table-spec)
    {:get {:parameters {:path (primary-key-parameters table-spec)}
           :handler (handlers/detail query-fn (keyword (q/hugsql-query-name table-spec :select-by-pk {:depluralize? true})))}
     :delete {:parameters {:path (primary-key-parameters table-spec)}
              :handler (handlers/delete query-fn (keyword (q/hugsql-query-name table-spec :delete {:depluralize? true})))}}]])

(comment

  (def t1-spec
    {:name    "users"
     :columns [["id" [:uuid] [[:primary-key]]]
               ["username" [:text]]]})

  (def t2-spec
    {:name    "users"
     :columns [["id" [:uuid]]
               ["username" [:text]]]
     :constraints [[:primary-key "id" "username"]]})

  
  )
