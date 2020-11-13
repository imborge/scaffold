(ns scaffold.core
  (:require [clojure.string :as str]
            [scaffold.postgres.core :as pg]
            [scaffold.postgres.query :as q]))

(defn generate-migration
  ([table-spec]
   (generate-migration table-spec (str "add-" (:name table-spec) "-table.up")))
  ([table-spec filename]
   (spit filename (pg/table-sql table-spec))))

(defn generate-queries
  ([table-spec]
   (generate-migration table-spec (str (:name table-spec) "-queries.sql")))
  ([table-spec filename]
   (let [insert-sql
         (q/insert table-spec (comp q/append-column-cast q/hugsql-var))

         select-sql
         (q/select table-spec)

         update-sql
         (q/update table-spec (comp q/append-column-cast q/hugsql-var))

         delete-sql
         (q/delete table-spec (comp q/append-column-cast q/hugsql-var))]
     (spit filename (str/join "\n\n" [insert-sql select-sql update-sql delete-sql])))))

