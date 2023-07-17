(ns scaffold.request-handlers
  (:refer-clojure :exclude [update])
  (:require [scaffold.util :as util]
            [scaffold.configuration :as configuration]))

(defn controller-ns [ns]
  (list 'ns ns
        (list :require '[ring.util.http-response :as response])))

(defn create [query-fn query-name & {:keys [defn?]
                                     :or   {defn? true}
                                     :as   opts}]
  (list (if defn? 'defn 'fn) 'create '[request]
        (list 'if (list '< 0 (list query-fn query-name '(:params request)))
              (list 'response/created)
              (list 'response/bad-request
                    {:error (str query-name " inserting failed")}))))

(defn index [query-fn query-name & {:keys [defn?]
                                    :or   {defn? true}
                                    :as   opts}]
  (list (if defn? 'defn 'fn) 'index '[request]
        (list 'response/ok (list query-fn query-name '(:params request)))))

(defn single [query-fn query-name & {:keys [defn?]
                                     :or   {defn? true}
                                     :as   opts}]
  (list (if defn? 'defn 'fn) 'single '[request]
        (list 'if-let ['entity (list query-fn query-name '(:params request))]
              (list 'response/ok 'entity)
              (list 'response/not-found))))

(defn update [query-fn query-name & {:keys [defn?]
                                     :or   {defn? true}
                                     :as   opts}]
  (list (if defn? 'defn 'fn) 'update '[request]
        (list 'if-let ['entity (list query-fn query-name '(:params request))]
              (list 'response/ok 'entity)
              (list 'response/bad-request
                    {:error (str query-name " did not return an entity")}))))

(defn delete [query-fn query-name & {:keys [defn?]
                                     :or   {defn? true}
                                     :as   opts}]
  (list (if defn? 'defn 'fn) 'delete '[request]
        (list 'if (list '< 0 (list query-fn query-name '(:params request)))
              (list 'response/no-content)
              (list 'response/bad-request
                    {:error (str query-name " did not delete any rows")}))))

(defn generate-index-str [configuration model]
  (let [query-naming-fn (:hugsql/query-naming-strategy configuration)
        query-name      (query-naming-fn (:name model) (first (:tables model)) :select)]
    (util/form->str
     (index (:hugsql/query-fn configuration)
            query-name))))

(defn generate-create-str [configuration model]
  (let [query-naming-fn (:hugsql/query-naming-strategy configuration)
        query-name (query-naming-fn (:name model) (first (:tables model)) :insert)]
    (util/form->str
     (create (:hugsql/query-fn configuration)
             query-name))))

(defn generate-single-str [configuration model]
  (let [query-naming-fn (:hugsql/query-naming-strategy configuration)]
    (util/form->str
     (single (:hugsql/query-fn configuration)
             (query-naming-fn (:name model) (first (:tables model)) :select-by-pk)))))

(defn generate-update-str [configuration model]
  (let [query-naming-fn (:hugsql/query-naming-strategy configuration)]
    (util/form->str
     (update (:hugsql/query-fn configuration)
             (query-naming-fn (:name model) (first (:tables model)) :update)))))

(defn generate-delete-str [configuration model]
  (let [query-naming-fn (:hugsql/query-naming-strategy configuration)]
    (util/form->str
     (delete (:hugsql/query-fn configuration)
             (query-naming-fn (:name model) (first (:tables model)) :delete)))))

(defn generate [configuration model actions]
  (let [filename
        (configuration/render-field configuration :controllers/filename (configuration/variables {:model model}))

        handlers
        {:index  (generate-index-str configuration model)
         :create (generate-create-str configuration model)
         :single (generate-single-str configuration model)
         :update (generate-update-str configuration model)
         :delete (generate-delete-str configuration model)}]

    {:filename filename
     :ns       (util/create-ns-from-path (util/get-src-dir filename) filename)
     :handlers
     (if (some #{:all} actions)
       handlers
       (select-keys handlers actions))}))
