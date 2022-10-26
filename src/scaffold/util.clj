(ns scaffold.util
  (:require [clojure.string :as str]))

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
  (if (.startsWith path src-dir)
    (let [path-without-src-dir (->> path
                                    (drop (count src-dir))
                                    remove-slash-prefix)]
      (-> path-without-src-dir
          (str/replace #"\..*$" "") ;; remove ext
          (str/replace #"\/" "."))) ;; convert / to .
    path))

(defn compute-request-handler-filename [dir config-filename table-spec]
  (cond
    config-filename
    (str (some-> dir remove-trailing-slashes)
         "/"
         config-filename)

    :else
    (str
     (some-> dir
             remove-trailing-slashes)
     "/"
     (create-filename (:name table-spec)))))
