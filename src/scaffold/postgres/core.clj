(ns scaffold.postgres.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [scaffold.postgres.constraints :as constraints]
   [scaffold.postgres.types :as types]))

(defn column-sql [[col-name col-type constraints
                   :as column-spec]]
  (let [col-type-str     (types/type col-type)
        constraints-strs (constraints/column-constraints constraints)]
    (str col-name " " col-type-str (when (seq constraints-strs)
                                     (str " " constraints-strs)))))

(defn table-sql [table]
  (str "CREATE TABLE " (:name table) " (\n"
       (str/join ",\n" (map column-sql (:columns table)))
       ");"))
