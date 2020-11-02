(ns scaffold.core
  (:require [clojure.string :as str]))

(def users {:table  "users"
            :columns [["id" :uuid [:pk [:default "uuid_v4_generate"] :not-null]]
                      ["username" :citext [:unique [:check "LEN(username) > 3"] :not-null]]
                      ["password" :text [:not-null]]]})

(def profile {:table   "profile"
              :columns [["user_id" :uuid [[:fk "users (id)"] :not-null]]
                        ["avatar_url" :url [:not-null]]]})
