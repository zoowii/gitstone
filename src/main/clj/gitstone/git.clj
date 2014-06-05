(ns gitstone.git
  (:require [clojure.java.shell :refer [sh]]))

(defn exec-cmd
  "执行shell命令"
  [cmd dir-path input-stream]
  (let [sh-res (apply sh (merge
                           (vec (.split cmd " "))
                           :out-enc :bytes
                           :in input-stream
                           :dir dir-path))]
    (:out sh-res)))