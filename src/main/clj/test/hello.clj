(ns test.hello
  (:import (clojure.lang Namespace))
  (:require [test.dummy :as dummy]
            [clojure.tools.namespace :as ns-tools]
            [clojure.tools.namespace.repl :as ns-repl]
            :reload))

(defn say-hi [name]
  (str (dummy/say-hi name) "hi"))

;(defn really-unload-namespace [^Namespace ns]
;  (let [ns-sym (symbol (.getName ns))
;        loaded-libs (.get (Var. "clojure.core") "*loaded-libs*")]
;    (dosync
;      (alter loaded-libs disj ns-sym))
;    (remove-ns (.getName ns))))

(defn reload-dummy
  []
  (require 'test.dummy :reload))
