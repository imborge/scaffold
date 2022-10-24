(ns scaffold.core
  (:require [clojure.string :as str]
            [scaffold.postgres.core :as pg]
            [scaffold.postgres.query :as q]
            [scaffold.reitit :as reitit]
            [scaffold.model :as m]))

(defn generate-migration
  [table-spec]
  (pg/table-sql table-spec))

(defn generate-hugsql-queries
  [table-spec]
  (let [insert-sql
        (str (q/hugsql-signature table-spec :insert {:depluralize? true})
             (q/insert table-spec (comp q/append-column-cast q/hugsql-var)))

        select-sql
        (str (q/hugsql-signature table-spec :select {:depluralize? true})
             (q/select table-spec))

        select-by-pk-sql
        (str (q/hugsql-signature table-spec :select-by-pk {:depluralize? true})
             (q/select table-spec))

        update-sql
        (str (q/hugsql-signature table-spec :update {:depluralize? true})
             (q/update table-spec (comp q/append-column-cast q/hugsql-var)))

        delete-sql
        (str (q/hugsql-signature table-spec :delete {:depluralize? true})
             (q/delete table-spec (comp q/append-column-cast q/hugsql-var)))]
    (str/join "\n\n" [insert-sql select-sql select-by-pk-sql update-sql delete-sql])))

(def sample-table-spec
  {:name "users"
   :columns [["id" [:uuid] [[:primary-key]]]
             ["email" [:citext]]]})

(def sample-configuration
  {:migrations/dir         "resources/migrations/"
   :migrations/overwrite?  false
   :reitit/insert?         true
   :reitit/routes-file     "" ;; file containing routes
   :reitit/routes-var      'routes ;; var containing routes vector
   :reitit/insert-path     [:__last :__after]
   ;; values:
   #_                      [[:__first :__before]
                            [:__first :__after]
                            [:__last :__before]
                            ["/users" :__after]
                            ["/users" :__before]]
   :hugsql/queries-dir     "resources/queries/"
   :hugsql/queries-file    "my-queries.sql"
   :hugsql/queries-append? false
   :hugsql/query-fn        'db.my-query-fn})

(defn scaffold [configuration table-spec]
  (let [migration (generate-migration table-spec)
        queries (generate-hugsql-queries table-spec)
        routes (reitit/routes table-spec (:hugsql/query-fn configuration))]
    {:migration migration
     :queries queries
     :routes routes}))
