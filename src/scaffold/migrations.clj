(ns scaffold.migrations
  (:require
   [scaffold.postgres.core :as pg]
   [scaffold.configuration :as configuration]
   [selmer.parser :as selmer]
   [clojure.string :as str]))

#_(defn default-filename-fn [table-spec]
    (str (.format (LocalDateTime/now)
                  (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
         "-create-" (:table table-spec) "-table.up.sql"))

(defn generate-migration
  [configuration model]
  {:name (selmer/render (:migrations/filename configuration) (configuration/variables {:model model}))
   :sql  (str/join "\n--;;\n" (map pg/table-sql (:tables model)))})
