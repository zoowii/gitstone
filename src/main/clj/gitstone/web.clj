(ns gitstone.web
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (java.util ArrayList))
  (:require [gitstone.db :as db]
            [gitstone.util :as util]))

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

(defn static-url
  [path]
  (HttpRouter/reverseUrl "static-file" (util/jlist* path)))

(defn js-url
  [path]
  (static-url (str "js/" path)))

(defn css-url
  [path]
  (static-url (str "css/" path)))

(defn img-url
  [path]
  (static-url (str "img/" path)))