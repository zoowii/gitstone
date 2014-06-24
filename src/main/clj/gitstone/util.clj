(ns gitstone.util
  (:import (java.util UUID Date ArrayList TimeZone)
           (com.zoowii.util DateUtil ClojureUtil)
           (com.zoowii.mvc.http HttpRequest HttpRouter)))

(defn uuid
  []
  (str (UUID/randomUUID)))

(defn now-timestamp
  []
  (-> (Date.)
      (.getTime)))

(def zh-cn-time-zone (TimeZone/getTimeZone "Asia/Shanghai"))

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

(defmacro clj-debug
  [& xs]
  `(ClojureUtil/debug [~@xs]))