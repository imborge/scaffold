(ns scaffold.core
  (:require [clojure.string :as str]
            [scaffold.postgres.core :as pg]
            [scaffold.postgres.query :as q]))

(defn generate-migration
  ([table-spec]
   (generate-migration table-spec (str "add-" (:name table-spec) "-table.up")))
  ([table-spec filename]
   (spit filename (pg/table-sql table-spec))))

(defn generate-hugsql-queries
  ([table-spec]
   (generate-migration table-spec {}))
  ([table-spec {:keys [filename]
                :or   {filename (str "add-" (:name table-spec) "-table.up")}
                :as   opts}]
   (let [insert-sql
         (str (q/hugsql-signature :insert (:name table-spec) {:depluralize? true})
              (q/insert table-spec (comp q/append-column-cast q/hugsql-var)))
         
         select-sql
         (str (q/hugsql-signature :select (:name table-spec) {:depluralize? true})
              (q/select table-spec))

         update-sql
         (str (q/hugsql-signature :update (:name table-spec) {:depluralize? true})
              (q/update table-spec (comp q/append-column-cast q/hugsql-var)))

         delete-sql
         (str (q/hugsql-signature :delete (:name table-spec) {:depluralize? true})
              (q/delete table-spec (comp q/append-column-cast q/hugsql-var)))]
     (spit filename (str/join "\n\n" [insert-sql select-sql update-sql delete-sql])))))

