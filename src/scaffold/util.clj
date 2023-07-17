(ns scaffold.util
  (:require [clojure.string :as str]
            [clojure.pprint]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn sanitize-filename [filename]
  (str/replace filename #"\-" "_"))

(defn create-filename [from-name]
  (str (sanitize-filename from-name) ".clj"))

(defn match-src-path [src-paths path]
  (reduce (fn [out src-path]
            (if (.startsWith path src-path)
              src-path
              out)) "" src-paths))

(defn remove-slash-prefix [path]
  (apply str (drop-while #{\/} path)))

(defn remove-trailing-slashes [path]
  (->> path
       reverse
       (drop-while #{\/})
       reverse
       (apply str)))

(defn create-ns-from-path
  "Calculates a namespace name given a `src-dir` and `path`

  Example:
  `src-dir` = src/clj
  `path` = src/clj/some/file.clj
  returns some.file"
  [src-dir path]
  (let [path
        (if (.startsWith path src-dir)
          (->> path
               (drop (count src-dir))
               remove-slash-prefix)
          path)]
    (-> path
        (str/replace #"\..*$" "") ;; remove ext
        (str/replace #"\/" ".")   ;; convert / to .
        symbol)))

(defn form->str
  "Return the a prettified string of `form` using clojure.pprint/code-dispatch

  Example usage:
  (code->str '(println \"Kek\")
  => \"(println \"kek\")"
  [form]
  (with-out-str
    (clojure.pprint/write
     form
     :dispatch clojure.pprint/code-dispatch)))

(defn deps-project? []
  (.exists (io/as-file "deps.edn")))

(defn src-dir?
  [dir file-whose-path-should-start-with-dir]
  (str/starts-with? file-whose-path-should-start-with-dir
                    dir))

(defn get-src-dir [src-file]
  (when (deps-project?)
    (first (filter #(src-dir? % src-file)
                   (:paths (edn/read-string (slurp (io/as-file "deps.edn"))))))))

#_(defn find-project-root-dir []
    (loop [cwd "."]
      (let [files (->> (file-seq cwd)
                       (filter #(.isFile %))
                       (filter
                        #(#{"deps.edn"     ;; deps.edn
                            "project.clj"} ;; leiningen
                          (.getName %))))]
        (if (seq files)))))
