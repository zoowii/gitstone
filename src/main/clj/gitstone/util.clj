(ns gitstone.util
  (:import (java.util UUID Date)))

(defn uuid
  []
  (str (UUID/randomUUID)))

(defn now-timestamp
  []
  (-> (Date.)
      (.getTime)))