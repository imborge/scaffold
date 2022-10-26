(ns scaffold.request-handlers
  (:refer-clojure :exclude [update])
  (:require [clojure.string :as str]
            [scaffold.util :as util]
            [scaffold.postgres.query :as q]))

(defn controller-ns [ns]
  (list 'ns ns
        (list :require '[ring.util.http-response :as response])))

(defn create [query-fn query & {:keys [defn?]
                                :or   {defn? true}
                                :as   opts}]
  (list (if defn? 'defn 'fn) 'create '[request]
        (list 'if (list < 0 (list query-fn query '(:params request)))
              (list 'ring.util.http-response/created)
              (list 'ring.util.http-response/bad-request
                    {:error (str query " inserting failed")}))))

(defn index [query-fn query & {:keys [defn?]
                               :or   {defn? true}
                               :as   opts}]
  (list (if defn? 'defn 'fn) 'index '[request]
        (list 'ring.util.http-response/ok (list
                                           query-fn query '(:params request)))))

(defn detail [query-fn query & {:keys [defn?]
                                :or   {defn? true}
                                :as   opts}]
  (list (if defn? 'defn 'fn) 'detail '[request]
        (list 'if-let ['entity (list query-fn query '(:params request))]
              (list 'ring.util.http-response/ok 'entity)
              (list 'ring.util.http-response/not-found))))

(defn update [query-fn query & {:keys [defn?]
                                :or   {defn? true}
                                :as   opts}]
  (list (if defn? 'defn 'fn) 'update '[request]
        (list 'if-let ['entity (list query-fn query '(:params request))]
              (list 'ring.util.http-response/ok 'entity)
              (list 'ring.util.http-response/bad-request
                    {:error (str query " did not return an entity")}))))

(defn delete [query-fn query & {:keys [defn?]
                                :or   {defn? true}
                                :as   opts}]
  (list (if defn? 'defn 'fn) 'delete '[request]
        (list 'if (list < 0 (list query-fn query '(:params request)))
              (list 'ring.util.http-response/no-content)
              (list 'ring.util.http-response/bad-request
                    {:error (str query " did not delete any rows")}))))

(defn generate [configuration table-spec actions]
  (let [filename
        (util/compute-request-handler-filename
         (:request-handler/dir configuration)
         (:request-handler/file configuration)
         table-spec)
        
        src-dir
        (util/match-src-path (get-in configuration [:deps :paths]) filename)

        query-naming-fn
        (:queries/naming-strategy configuration)
        
        handlers
        {:index  (index (:query-fn configuration)
                        (query-naming-fn table-spec :select))
         :detail (detail (:query-fn configuration)
                         (query-naming-fn table-spec :select-by-pk))
         :update (update (:query-fn configuration)
                         (query-naming-fn table-spec :update))
         :delete (delete (:query-fn configuration)
                         (query-naming-fn table-spec :delete))}]
    {:filename filename
     :ns       (util/create-ns-from-path src-dir filename)
     :handlers
     (if (some #{:all} actions)
       handlers
       (select-keys handlers actions))}))
