(ns scaffold.postgres.core
  (:require [clojure.string :as str]
            [scaffold.postgres.constraints :as constraints]))

(def simple-types
  {:bigint        "BIGINT"
   :bigserial     "BIGSERIAL"
   :bit           "BIT"
   :varbit        "VARBIT"
   :boolean       "BOOLEAN"
   :box           "BOX"
   :bytea         "BYTEA"
   :varchar       "VARCHAR"
   :char          "CHAR"
   :cidr          "CIDR"
   :circle        "CIRCLE"
   :date          "DATE"
   :double        "FLOAT8"
   :inet          "INET"
   :integer       "INTEGER"
   :interval      "INTERVAL"
   :line          "LINE"
   :lseg          "LSEG"
   :macaddr       "MACADDR"
   :money         "MONEY"
   :numeric       "NUMERIC"
   :path          "PATH"
   :point         "POINT"
   :polygon       "POLYGON"
   :real          "REAL"
   :smallint      "SMALLINT"
   :serial        "SERIAL"
   :text          "TEXT"
   :time          "TIME"
   :timetz        "TIMETZ"
   :timestamp     "TIMESTAMP"
   :timestamptz   "TIMESTAMPTZ"
   :tsquery       "TSQUERY"
   :tsvector      "TSVECTOR"
   :txid_snapshot "TXID_SNAPSHOT"
   :uuid          "UUID"
   :xml           "XML"})

(defn generate-column-sql [[col-name col-type constraints
                            :as column]]
  (let [col-type-str     (cond
                           (keyword? col-type) (get simple-types col-type))
        constraints-strs (constraints/generate-column-constraints constraints)]
    (str col-name " " col-type-str (when (seq constraints-strs)
                                     (str " " constraints-strs)))))

(defn generate-table-sql [table]
  (str "CREATE TABLE " (:name table) " (\n"
       (str/join ",\n" (map generate-column-sql (:columns table)))
       ");"))
