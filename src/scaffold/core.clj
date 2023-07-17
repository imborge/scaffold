(ns scaffold.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint]
            [clojure.string :as str]
            [scaffold.migrations :as migrations]
            [scaffold.postgres.query :as q]
            [scaffold.reitit :as reitit]
            [scaffold.request-handlers :as handlers]))

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
        migration     (migrations/generate-migration configuration table-spec)
        queries       (q/generate-hugsql-queries table-spec)
        handlers      (handlers/generate configuration table-spec [:all])
        routes        (reitit/routes table-spec handlers)]
    (println "Scaffolding...")
    (save-migration! configuration migration)
    (save-queries! configuration queries)
    (save-request-handlers! configuration handlers)
    (save-routes! configuration table-spec routes)
    (println "Scaffolding done.")))
