(ns scaffold.configuration
  (:require [clojure.edn :as edn]
            [selmer.parser :as selmer]
            [scaffold.util :as util]))

(def template
  {:migrations/overwrite?        false
   :migrations/filename          "{% verbatim %}resources/migrations/{{current-time|date:\"yyyyMMddHHmmssSSS\"}}-create-{{model-name}}-tables-up.sql{% endverbatim %}"
   :reitit/routes-file           "{{project-root-ns-dir}}/{% verbatim %}{{model-name}}/routes.clj{% endverbatim %}"
   :controllers/filename         "{{project-root-ns-dir}}/{% verbatim %}{{model-name}}/controller.clj{% endverbatim %}"
   :hugsql/queries-file          "{% verbatim %}resources/sql-queries/{{model-name}}.sql{% endverbatim %}"
   :hugsql/queries-append?       false
   :hugsql/query-fn              'db.my-query-fn
   :hugsql/query-naming-strategy 'scaffold.postgres.query/default-query-naming-strategy})

(defn load [filename]
  (let [config (-> (edn/read-string (slurp filename))
                   (update :hugsql/query-naming-strategy requiring-resolve))]
    config))

(defn variables [{:keys [configuration options model]}]
  {:current-time (java.util.Date.)
   :model-name   (:name model)})

(defn render [config variables]
  (str (util/form->str
        (into {}
              (map (fn [[k v]]
                     [k (if (string? v)
                          (selmer/render v variables)
                          v)]) config))) "\n"))

(defn render-field [config kw env]
  (selmer/render (config kw) env))
