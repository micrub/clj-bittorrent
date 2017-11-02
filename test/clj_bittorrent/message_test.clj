(ns clj-bittorrent.message-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.message :as msg]
            [clj-bittorrent.peer :as peer]))

(deftest message-method-test
  (is (= 4 (count (msg/keep-alive))))
  (is (= 5 (count (msg/choke))))
  (is (= 5 (count (msg/unchoke))))
  (is (= 5 (count (msg/interested))))
  (is (= 5 (count (msg/not-interested))))
  (is (= 9 (count (msg/have 1))))
  (is (= 6 (count (msg/bitfield [1]))))
  (is (= 17 (count (msg/request 1 1 1))))
  (is (= 14 (count (msg/piece 1 1 [1]))))
  (is (= 17 (count (msg/cancel 1 1 1))))
  (is (= 7 (count (msg/port 1)))))

(deftest recv-message-test
  (is (= {:id :keep-alive} (msg/recv [0x00 0x00 0x00 0x00])))
  (is (= {:id :choke} (msg/recv [0x00 0x00 0x00 0x00 0x00])))
  (is (= {:id :unchoke} (msg/recv [0x00 0x00 0x00 0x00 0x01])))
  (is (= {:id :interested} (msg/recv [0x00 0x00 0x00 0x00 0x02])))
  (is (= {:id :not-interested} (msg/recv [0x00 0x00 0x00 0x00 0x03])))
  (is (= {:id :have :index 6969} (msg/recv [0x00 0x00 0x00 0x00 0x04 0x1b 0x39])))
  (is (= {:id :bitfield :indices #{1}} (msg/recv [0x00 0x00 0x00 0x00 0x05 0x40])))
  (is (= {:id :request :index 1 :begin 2 :length 3}
         (msg/recv (concat
                     [0x00 0x00 0x00 13]
                     [0x06]
                     [0x00 0x00 0x00 0x01]
                     [0x00 0x00 0x00 0x02]
                     [0x00 0x00 0x00 0x03]))))
  (is (= {:id :piece :index 7 :begin 12 :block (seq [0x12 0x34 0x56 0x78 0x9a])}
         (msg/recv (concat
                     [0x00 0x00 0x00 0x0D]
                     [0x07]
                     [0x00 0x00 0x00 0x07]
                     [0x00 0x00 0x00 0x0c]
                     [0x12 0x34 0x56 0x78 0x9a]))))
  (is (= {:id :cancel :index 1 :begin 2 :length 3}
         (msg/recv (concat
                     [0x00 0x00 0x00 13]
                     [0x08]
                     [0x00 0x00 0x00 0x01]
                     [0x00 0x00 0x00 0x02]
                     [0x00 0x00 0x00 0x03]))))
  (is (= {:id :port :port 6969}
         (msg/recv (concat
                     [0x00 0x00 0x00 0x03]
                     [0x09]
                     [0x1b 0x39])))))

(def choked-client (assoc-in peer/connection-default-state [:client :choked] true))
(def unchoked-client (assoc-in peer/connection-default-state [:client :choked] false))
(def interested-remote (assoc-in peer/connection-default-state [:peer :interested] true))
(def uninterested-remote (assoc-in peer/connection-default-state [:peer :interested] false))

(deftest apply-msg-test
  (is (= {} (msg/apply-msg :keep-alive {})))
  (is (= choked-client (msg/apply-msg :choke unchoked-client)))
  (is (= choked-client (msg/apply-msg :choke choked-client)))
  (is (= unchoked-client (msg/apply-msg :unchoke choked-client)))
  (is (= unchoked-client (msg/apply-msg :unchoke unchoked-client)))
  (is (= interested-remote (msg/apply-msg :interested uninterested-remote)))
  (is (= interested-remote (msg/apply-msg :interested interested-remote)))
  (is (= uninterested-remote (msg/apply-msg :not-interested interested-remote)))
  (is (= uninterested-remote (msg/apply-msg :not-interested uninterested-remote))))