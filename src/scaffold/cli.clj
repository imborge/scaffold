(ns scaffold.cli
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint]
            [scaffold.util :as util]
            [scaffold.postgres.query :as query]
            [scaffold.configuration :as configuration]
            [scaffold.migrations :as migrations]
            [scaffold.request-handlers :as handlers])
  (:gen-class))

(defn generate-all? [opt-id]
  (fn [parsed-opts]
    (or (:all parsed-opts)
        (get parsed-opts opt-id))))

(def cli-options
  [["-D" "--destructive" "Write scaffolded code to files"
    :default false]
   [nil "--config FILE" "Set the configuration file"
    :default "./scaffold-config.edn"
    ;; TODO: Validate config format
    :validate [#(.exists (io/as-file %)) "Configuration file does not exist"]]
   ["-c" "--create" "Generate code for create request handler"
    :default false
    :default-fn (generate-all? :create)]
   ["-i" "--index" "Generate code for read index request handler"
    :default false
    :default-fn (generate-all? :index)]
   ["-s" "--single" "Generate code for read single request handler"
    :default false
    :default-fn (generate-all? :single)]
   ["-u" "--update" "Generate code for update request handler"
    :default false
    :default-fn (generate-all? :update)]
   ["-d" "--delete" "Generate code for delete request handler"
    :default false
    :default-fn (generate-all? :delete)]
   ["-m" "--migrations" "Generate database migrations"
    :default false
    :default-fn (generate-all? :migrations)]
   ["-q" "--queries" "Generate SQL queries"
    :default false
    :default-fn (generate-all? :queries)]
   ["-r" "--routes" "Generate routes"
    :default false
    :default-fn (generate-all? :routes)]
   ["-A" "--all" "Generate everything"
    :default false]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Welcome to imborge/scaffold!"
        ""
        "Usage: scaffold [options] init|FILE"
        ""
        "Arguiments:"
        "  init: Create a new configuration file"
        "  FILE: Scaffold using FILE as model"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (= "init" (first arguments))
      {:action  :init
       :options options}

      (<= 1 (count arguments))
      {:action  :scaffold
       :options options
       :models  arguments}

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(def generators {:routes     (fn [_configuration model]
                               "Not implemented")
                 :migrations #'migrations/generate-migration
                 :queries    #'query/generate-hugsql-queries})

(defn run-scaffold
  [options [model-filename]]
  (let [model-file (io/as-file model-filename)
        config     (configuration/load (:config options))]
    (if (.exists model-file)
      (let [model (edn/read-string (slurp model-file))]
        (doseq [generator (set (keys generators))]
          (when (options generator)
            (let [generate-fn (generators generator)]
              (println (generate-fn config model))
              (println)
              (println))))
        (println (handlers/generate config model (keys (filter (fn [[k v]] (true? v))
                                                               (select-keys options [:index :create :single :update :delete])))))
        (println))
      (exit 1 (str "Cannot read model. File does not exist: "
                   model-filename)))))

(defn init-config [options]
  (print "Project root-namespace directory (e.g. src/scaffold): ")
  (flush)
  (let [project-root-ns-dir (util/remove-trailing-slashes (read-line))]
    (spit (:config options)
          (configuration/render configuration/template {:project-root-ns-dir project-root-ns-dir}))
    (println "Initialized configuration file " (:config options))))

(defn -main [& args]
  (let [{:keys [action options exit-message ok? models]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (condp = action
        :init     (init-config options)
        :scaffold (if (.exists (io/as-file (:config options)))
                    (run-scaffold options models)
                    (exit 1
                          (str "ERROR: Configuration file: " (:config options)
                               " doesn't exist.\n"
                               "Tips: You can initialize a configuration file using the following command:\n"
                               "  scaffold [options] init\n"
                               "For more help, run:\n"
                               "  scaffold --help")))))))
