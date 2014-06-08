(ns gitstone.util
  (:import (java.util UUID Date)
           (com.zoowii.util DateUtil)))

(defn uuid
  []
  (str (UUID/randomUUID)))

(defn now-timestamp
  []
  (-> (Date.)
      (.getTime)))

(defn format-date
  ([date]
   (DateUtil/dateStringFormat date))
  ([date fmt]
   (DateUtil/formatDate date fmt))
  ([date fmt time-zone]
   (DateUtil/formatDate date fmt time-zone)))