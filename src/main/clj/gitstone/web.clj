(ns gitstone.web
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (java.util ArrayList))
  (:require [gitstone.db :as db]))

(defn redirect
  [^HttpResponse response url]
  (.redirect response url))

(defn url-for
  [route-name & params]
  (HttpRouter/reverseUrl route-name (ArrayList. (vec params))))

(defn login-as-user
  [^HttpRequest req user]
  (.session req "username" (:username user)))

(defn logout
  [^HttpRequest req]
  (.clearSession req))

(defn current-user
  [^HttpRequest req]
  (let [username (.session req "username")]
    (if username
      (db/find-user-by-username username))))