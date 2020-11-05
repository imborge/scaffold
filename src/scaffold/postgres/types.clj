(ns scaffold.postgres.types
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::numeric
  (s/cat :precision pos-int?
         :scale (s/? nat-int?)))

(defn generate-bit [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "BIT(" n ")")
    (str "BIT")))

(defn generate-varbit [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "VARBIT(" n ")")
    (str "VARBIT")))

(defn generate-char [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "CHAR(" n ")")
    (str "CHAR")))

(defn generate-varchar [[n]]
  {:pre [(s/valid? (s/nilable pos-int?) n)]}
  (if n
    (str "VARCHAR(" n ")")
    (str "VARCHAR")))

(defn generate-numeric [[precision scale :as ps]]
  {:pre [(s/valid? (s/* ::numeric) ps)]}
  (if scale
    (str "NUMERIC(" precision "," scale ")")
    (if precision
      (str "NUMERIC(" precision ")")
      (str "NUMERIC"))))

(defn generate-time [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIME(" precision ")")
    (str "TIME")))

(defn generate-timetz [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIME(" precision ") WITH TIME ZONE")
    (str "TIME WITH TIME ZONE")))

(defn generate-timestamp [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIMESTAMP(" precision ")")
    (str "TIMESTAMP")))

(defn generate-timestamptz [[precision]]
  {:pre [(s/valid? (s/nilable #(<= 0 % 6)) precision)]}
  (if precision
    (str "TIMESTAMP(" precision ") WITH TIME ZONE")
    (str "TIMESTAMP WITH TIME ZONE")))

(def type->generator
  {:bigint        (constantly "BIGINT")
   :bigserial     (constantly "BIGSERIAL")
   :bit           generate-bit
   :varbit        generate-varbit
   :boolean       (constantly "BOOLEAN")
   :box           (constantly "BOX")
   :bytea         (constantly "BYTEA")
   :char          generate-char
   :varchar       generate-varchar
   :cidr          (constantly "CIDER")
   :circle        (constantly "CIRCLE")
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
   :numeric       generate-numeric
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
   :time          generate-time
   :timetz        generate-timetz
   :timestamp     generate-timestamp
   :timestamptz   generate-timestamptz
   :tsquery       (constantly "TSQUERY") 
   :tsvector      (constantly "TSVECTOR")
   :txid_snapshot (constantly "TXID_SNAPSHOT")
   :uuid          (constantly "UUID")
   :xml           (constantly "XML")})

(defn generate-type
  ([pgtype-vec]
   (generate-type pgtype-vec type->generator))
  ([[pgtype & additional] type->generator-map]
   (let [additional  (vec additional)
         generate-fn (type->generator-map pgtype)]
     (if generate-fn
       (generate-fn additional)
       :invalid-type))))
