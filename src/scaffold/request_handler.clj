(ns scaffold.request-handler
  (:refer-clojure :exclude [update]))

(defn create [query query-fn]
  (list 'fn 'create '[request]
        (list 'if-let ['entity (list query-fn query '(:params request))]
              (list 'ring.util.http-response/created 'entity)
              (list 'ring.util.http-response/internal-server-error
                    {:error (str query " did not return an entity")}))))

(defn index [query query-fn]
  (list 'fn 'index '[request]
        (list 'ring.util.http-response/ok (list query-fn query '(:params request)))))

(defn detail [query query-fn]
  (list 'fn 'detail '[request]
        (list 'if-let ['entity (list query-fn query '(:params request))]
              (list 'ring.util.http-response/ok 'entity)
              (list 'ring.util.http-response/not-found))))

(defn update [query query-fn]
  (list 'fn 'update '[request]
        (list 'if-let ['entity (list query-fn query '(:params request))]
              (list 'ring.util.http-response/ok 'entity)
              (list 'ring.util.http-response/bad-request
                    {:error (str query " did not return an entity")}))))

(defn delete [query query-fn]
  (list 'fn 'delete '[request]
        (list 'if (list 'seq (list query-fn query '(:params request)))
              (list 'ring.util.http-response/no-content)
              (list 'ring.util.http-response/bad-request
                    {:error (str query " did not delete any rows")}))))


