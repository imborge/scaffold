(ns scaffold.core
  (:require [clojure.string :as str]
            [scaffold.postgres.core :as pg]
            [scaffold.postgres.query :as q]))

(defn generate-migration [table-spec filename]
  (spit filename (pg/generate-table-sql table-spec)))

(defn generate-queries [table-spec filename]
  (let [insert-sql
        (q/generate-insert table-spec (comp q/append-column-cast q/prepare-hugsql-val))

        select-sql
        (q/generate-select table-spec)

        update-sql
        (q/generate-update table-spec (comp q/append-column-cast q/prepare-hugsql-val))

        delete-sql
        (q/generate-delete table-spec (comp q/append-column-cast q/prepare-hugsql-val))]
    (spit filename (str/join "\n\n" [insert-sql select-sql update-sql delete-sql]))))

