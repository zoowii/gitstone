(ns test.hello
  (:require [test.dummy :as dummy]))

(defn say-hi [name]
  (dummy/say-hi name))
