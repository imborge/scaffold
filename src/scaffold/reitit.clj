(ns scaffold.reitit
  (:require
   [clojure.string :as str]
   [scaffold.model :as model]
   [scaffold.postgres.constraints :as c]
   [scaffold.postgres.query :as q]
   [scaffold.postgres.types :as t]
   [scaffold.request-handlers :as handlers]))

(defn primary-key-path
  "Generates the reitit resource path for `detail` and `delete`.

  The path is generated from the primary key columns, e.g.
  Given a table-spec with `id` as the primary key returns
  \"/:id\"

  Given a table-spec with `id` and `email` as primary keys returns
  \"/:id/:email\""
  [table-spec]
  (let [primary-keys (model/find-primary-key-columns table-spec)]
    (str "/" (str/join "/" (map keyword (c/column-names-from-constraint primary-keys))))))

(defn primary-key-parameters [table-spec]
  (let [primary-keys (model/find-primary-key-columns table-spec)
        column-names (c/column-names-from-constraint primary-keys)]
    (into
     {}
     (map
      (fn [column-name]
        (let [[_ [column-type] _] (model/find-column-by-name column-name (:columns table-spec))]
          [(keyword column-name)
           (t/pgtype->clj-predicate column-type)]))
      column-names))))

(defn body-parameters [table-spec]
  (let [primary-key-column-names (-> table-spec
                                     model/find-primary-key-columns
                                     c/column-names-from-constraint)]
    (into
     {}
     (map
      (fn [column-name]
        (let [[_ [column-type] _] (model/find-column-by-name column-name (:columns table-spec))]
          [(keyword column-name)
           (t/pgtype->clj-predicate column-type)]))
      (->> (:columns table-spec)
           (map first) ;; column name
           (remove (set primary-key-column-names)))))))

(defn routes [table-spec handlers]
  [(str "/" (:name table-spec))
   ["/"
    {:get (when (get-in handlers [:handlers :index])
            {:handler (symbol (name (:ns handlers)) "index")})
     
     :post (when (get-in handlers [:handlers :create])
             {:parameters {:body (body-parameters table-spec)}
              :handler    (symbol (name (:ns handlers)) "create")})}]
   
   [(primary-key-path table-spec)
    {:get (when (get-in handlers [:handlers :detail])
            {:parameters {:path (primary-key-parameters table-spec)}
             :handler    (symbol (name (:ns handlers)) "details")})
     
     :delete (when (get-in handlers [:handlers :delete])
               {:parameters {:path (primary-key-parameters table-spec)}
                :handler    (symbol (name (:ns handlers)) "delete")})}]])

(comment

  (def t1-spec
    {:name    "users"
     :columns [["id" [:uuid] [[:primary-key]]]
               ["username" [:text]]]})

  (def t2-spec
    {:name    "users"
     :columns [["id" [:uuid]]
               ["username" [:text]]]
     :constraints [[:primary-key "id" "username"]]})

  
  )
