(ns scaffold.postgres.types
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str])
  (:refer-clojure :exclude [char time type]))

(s/def ::numeric
  (s/cat :precision pos-int?
         :scale (s/? nat-int?)))

(defn bit [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "BIT(" n ")")
    (str "BIT")))

(defn varbit [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "VARBIT(" n ")")
    (str "VARBIT")))

(defn char [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "CHAR(" n ")")
    (str "CHAR")))

(defn varchar [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "VARCHAR(" n ")")
    (str "VARCHAR")))

(defn numeric [[precision scale :as ps]]
  {:pre [(s/valid? (s/* ::numeric) ps)]}
  (if scale
    (str "NUMERIC(" precision "," scale ")")
    (if precision
      (str "NUMERIC(" precision ")")
      (str "NUMERIC"))))

(defn time [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIME(" precision ")")
    (str "TIME")))

(defn timetz [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIME(" precision ") WITH TIME ZONE")
    (str "TIME WITH TIME ZONE")))

(defn timestamp [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIMESTAMP(" precision ")")
    (str "TIMESTAMP")))

(defn timestamptz [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIMESTAMP(" precision ") WITH TIME ZONE")
    (str "TIMESTAMP WITH TIME ZONE")))

(def type->generator
  {:bigint        (constantly "BIGINT")
   :bigserial     (constantly "BIGSERIAL")
   :bit           bit
   :varbit        varbit
   :boolean       (constantly "BOOLEAN")
   :box           (constantly "BOX")
   :bytea         (constantly "BYTEA")
   :char          char
   :varchar       varchar
   :cidr          (constantly "CIDER")
   :circle        (constantly "CIRCLE")
   :citext        (constantly "CITEXT")
   :date          (constantly "DATE")
   :double        (constantly "DOUBLE PRECISION")
   :inet          (constantly "INET")
   :integer       (constantly "INTEGER")
   :interval      (constantly "INTERVAL")
   :json          (constantly "JSON")
   :jsonb         (constantly "JSONB")
   :line          (constantly "LINE")
   :lseg          (constantly "LSEG")
   :macaddr       (constantly "MACADDR")
   :macaddr8      (constantly "MACADDR8")
   :money         (constantly "MONEY")
   :numeric       numeric
   :path          (constantly "PATH")
   :pg_lsn        (constantly "PG_LSN")
   :pg_snapshot   (constantly "PG_SNAPSHOT")
   :point         (constantly "POINT")
   :polygon       (constantly "POLYGON")
   :real          (constantly "REAL")
   :smallint      (constantly "SMALLINT")
   :smallserial   (constantly "SMALLSERIAL")
   :serial        (constantly "SERIAL")
   :text          (constantly "TEXT")
   :time          time
   :timetz        timetz
   :timestamp     timestamp
   :timestamptz   timestamptz
   :tsquery       (constantly "TSQUERY") 
   :tsvector      (constantly "TSVECTOR")
   :txid_snapshot (constantly "TXID_SNAPSHOT")
   :uuid          (constantly "UUID")
   :xml           (constantly "XML")})

(defn type
  ([pgtype-vec]
   (type pgtype-vec type->generator))
  ([[pgtype & additional] type->generator-map]
   (let [additional  (vec additional)
         generate-fn (type->generator-map pgtype)]
     (if generate-fn
       (generate-fn additional)
       :invalid-type))))
