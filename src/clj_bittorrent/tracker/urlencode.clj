(ns clj-bittorrent.tracker.urlencode
  "Make url-encoded byte arrays."
  (:require [schema.core :as schema]
            [clj-bittorrent.math.binary :as bin]))

(defn- char-range-integers [a b]
  {:pre [(< (int (char a)) (int (char b)))]}
  (set (range (int (char a))
              (inc (int (char b))))))

(def ^:private digits (char-range-integers \0 \9))

(def ^:private alpha
  (clojure.set/union
    (set (range (int \A) (inc (int \Z))))
    (set (range (int \a) (inc (int \z))))))

(def ^:private punct
  (set (map int #{\. \- \_ \~})))

(defn- allowed-raw? [b]
  (or
    (digits (int b))
    (alpha (int b))
    (punct (int b))))

(def UrlEncodedByte
  (schema/constrained [Character] #(#{1 3} (count (seq %)))))

(schema/defn urlencode-byte :- UrlEncodedByte
  [b :- bin/UnsignedByte]
  (let [ib (int b)
        result
           (if (allowed-raw? ib)
             (list (char b))
             (seq (str "%" (bin/hexbyte (int b)))))]
    result))

(schema/defn urlencode :- schema/Str
  "Url-encodes a seq of bytes. Makes no assumptions about encoding of strings."
  [s :- bin/ByteArray]
  (->>
    s
    (seq)
    (map bin/ubyte)
    (mapcat urlencode-byte)
    (apply str)))
