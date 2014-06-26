(ns gitstone.web
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (java.util ArrayList)
           (java.io InputStream)
           (com.zoowii.util FileUtil))
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

(defn html-render
  "处理视图函数返回的内容,作为给http response的map返回,默认content-type是text/html; charset=UTF-8"
  [out]
  (cond
    (string? out)
    {:body         out
     :status       200
     :content-type "text/html; charset=UTF8"}
    (map? out)
    out
    :else
    out))

(defn response
  "处理回复,把返回内容输出到客户端
  TODO: 目前不能处理byte[]的body,也没有post-filters"
  [^HttpResponse res out]
  (let [{status       :status
         body         :body
         content-type :content-type
         headers      :headers} (if (string? out)
                                  (html-render out)
                                  out)]
    (if (number? status)
      (.setStatus res status))
    (if (string? content-type)
      (.setContentType res content-type))
    (if (map? headers)
      (for [[k v] (seq headers)]
        (.setHeader res (name k) (name v))))
    (cond
      (string? body) (.append res body)
      (instance? InputStream body) (FileUtil/writeFullyStream body (.getOutputStream res)))))