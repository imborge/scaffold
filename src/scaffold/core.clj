(ns scaffold.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [scaffold.model :as m]
            [scaffold.postgres.core :as pg]
            [scaffold.postgres.query :as q]
            [scaffold.reitit :as reitit]
            [scaffold.request-handlers :as handlers])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(defn generate-migration
  [configuration table-spec]
  {:name ((:migrations/filename-fn configuration) table-spec)
   :sql  (pg/table-sql table-spec)})

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
   :migrations/filename-fn (fn [table-spec]
                             (str (.format (LocalDateTime/now)
                                           (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
                                  "-create-" (:name table-spec) "-table.up.sql"))
   :reitit/routes-file     "routes.clj"
   :request-handler/file   nil
   :request-handler/dir    "src/imborge/kit_test/web/controllers"
   :hugsql/queries-file    "my-queries.sql"
   :hugsql/queries-append? false
   :hugsql/query-fn        'db.my-query-fn
   :queries/naming-strategy (fn [table-spec action]
                              (keyword
                               (q/hugsql-query-name table-spec action {:depluralize? true})))})

(defn default-filename-fn [table-spec]
  (str (.format (LocalDateTime/now)
                (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
       "-create-" (:name table-spec) "-table.up.sql"))

(defn default-query-naming-strategy
  [table-spec action]
  (keyword
   (q/hugsql-query-name table-spec action {:depluralize? true})))

(def sample-configuration-2
  {:migrations/dir          "resources/migrations/"
   :migrations/overwrite?   false
   :migrations/filename-fn  default-filename-fn
   :reitit/routes-file      "src/test/routes.clj"
   :request-handler/file    nil
   :request-handler/dir     "src/test/controllers"
   :hugsql/queries-file     "resources/test.sql"
   :hugsql/queries-append?  false
   :hugsql/query-fn         'db.my-query-fn
   :queries/naming-strategy default-query-naming-strategy})

(defn save-migration! [configuration migration]
  (let [migration-filepath (str (:migrations/dir configuration) (:name migration))]
    (println "Saving migration to" migration-filepath)
    (spit migration-filepath (:sql migration))
    (println "Done")))

(defn save-queries! [configuration queries]
  (println "Saving queries to" (:hugsql/queries-file configuration))
  (let [queries-file-exists? (.exists (io/file (:hugsql/queries-file configuration)))
        append?              (:hugsql/queries-append? configuration)]
    (if (and queries-file-exists? (not append?))
      (throw (ex-info "File already exists and :hugsql/queries-append? is set to false" {}))
      (do
        (spit (:hugsql/queries-file configuration) queries :append true)
        (println "Done")))))

(defn save-request-handlers! [configuration handlers]
  (spit
   (:filename handlers)
   (str (with-out-str
          (clojure.pprint/write
           (handlers/controller-ns (:ns handlers))
           :dispatch clojure.pprint/code-dispatch))
        "\n\n"
        (str/join
         "\n\n"
         (map
          (fn print-handler [handler]
            (with-out-str
              (clojure.pprint/write
               handler
               :dispatch clojure.pprint/code-dispatch)))
          (vals (:handlers handlers)))))
   :append (:request-handler/append? configuration)))

(defn save-routes! [configuration table-spec routes]
  (let [filename     (:reitit/routes-file configuration)
        file-exists? (.exists (io/file filename))]
    (println "Saving routes to" filename)
    (cond
      file-exists?
      (do
        (spit
         filename
         (str
          "\n"
          (with-out-str
            (clojure.pprint/write
             (list 'def (symbol (str (:name table-spec) "-routes")) routes)
             :dispatch clojure.pprint/code-dispatch)))
         :append true)
        (println "Done."))

      :else
      (throw (ex-info "File doesn't exist. Error saving routes" {:configuration configuration
                                                                 :routes        routes})))))

(defn scaffold! [configuration table-spec]
  (let [deps          (when (.exists (io/file "deps.edn"))
                        (edn/read-string (slurp "deps.edn")))
        configuration (assoc configuration :deps deps)
        migration     (generate-migration configuration table-spec)
        queries       (generate-hugsql-queries table-spec)
        handlers      (handlers/generate configuration table-spec [:all])
        routes        (reitit/routes table-spec handlers)]
    (println "Scaffolding...")
    (save-migration! configuration migration)
    (save-queries! configuration queries)
    (save-request-handlers! configuration handlers)
    (save-routes! configuration table-spec routes)
    (println "Scaffolding done.")))
