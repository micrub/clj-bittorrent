(ns clj-bittorrent.tracker
  "Interact with tracker servers."
  (:require [clj-http.client :as client]
            [clj-bencode.core :as b]
            [clj-bittorrent.urlencode :as u]
            [clj-bittorrent.binary :as bin]))

(defn- decode-peer-binary-entry [s]
  (let [[ip port] (split-at 4 s)]
    {:ip (bin/ipv4-address (seq (map bin/ubyte ip)))
     :port (BigInteger. (byte-array (seq port)))}))

(defn- decode-peers-binary
  [s]
  {:post [(every? map? %)
          (every? some? (map :ip %))
          (every? some? (map :port %))]}
  (->> s
       (seq)
       (partition 6)
       (map decode-peer-binary-entry)))

(defn- tracker-response
  [m]
  {:pre [(some? m)]}
  (-> m
    (:body)
    (byte-array)
    (b/decode)
    (update-in ["peers"] decode-peers-binary)))

(defn- tracker-request
  "Create an HTTP request map from the info in map m."
  [url m]
  ;; clj-http will url-encode our query params for us,
  ;; but it encodes UTF-16, which trackers like opentracker
  ;; don't like. They expect UTF-8.
  {:method :get
   :url (str url
             "?info_hash=" (u/urlencode (:info-hash m))
             "&peer_id=" (u/urlencode (:peer-id m)))
   :query-params (select-keys m [:port
                                 :uploaded
                                 :downloaded
                                 :left
                                 :compact
                                 :event
                                 :ip
                                 :numwant])})

(defn announce
  "Announce yourself to the tracker for torrent map m."
  [url m]
  {:pre [(some? m)
         (= 20 (count (:info-hash m)))]}
  (->> m
    (tracker-request url)
    (client/request)
    (tracker-response)))

