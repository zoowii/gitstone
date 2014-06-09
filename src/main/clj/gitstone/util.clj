(ns gitstone.util
  (:import (java.util UUID Date ArrayList)
           (com.zoowii.util DateUtil)
           (com.zoowii.mvc.http HttpRequest HttpRouter)))

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


(defn jlist
  [col]
  (ArrayList. (vec col)))

(defn jlist*
  [& col]
  (jlist col))
