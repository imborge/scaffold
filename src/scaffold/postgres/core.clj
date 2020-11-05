(ns scaffold.postgres.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [scaffold.postgres.constraints :as constraints]
   [scaffold.postgres.types :as types]))

(defn generate-column-sql [[col-name col-type constraints
                            :as column]]
  (let [col-type-str     (types/generate-type col-type)
        constraints-strs (constraints/generate-column-constraints constraints)]
    (str col-name " " col-type-str (when (seq constraints-strs)
                                     (str " " constraints-strs)))))

(defn generate-table-sql [table]
  (str "CREATE TABLE " (:name table) " (\n"
       (str/join ",\n" (map generate-column-sql (:columns table)))
       ");"))
