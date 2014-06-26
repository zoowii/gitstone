(ns gitstone.view-util
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse))
  (:require [clojure.tools.logging :as logging]
            [gitstone.web :as web]
            [gitstone.util :as util]
            [gitstone.db :as db]
            [gitstone.repo-dao :as repo-dao]
            [gitstone.user-dao :as user-dao]))

(defn asset-login-wrapper
  "确保用户已经登录"
  ([handler]
   (asset-login-wrapper handler #(web/url-for "login_page")))
  ([handler redirect-url-fn]
   (fn [req res]
     (if (nil? (web/current-user req))
       (web/redirect res (redirect-url-fn))
       (handler req res)))))

(defn asset-admin-wrapper
  "确保用户是管理员"
  ([handler]
   (asset-admin-wrapper handler #(web/url-for "login_page")))
  ([handler redirect-url-fn]
   (fn [req res]
     (let [cur-user (web/current-user req)]
       (if (or (nil? cur-user)
               (not (user-dao/is-admin-user cur-user)))
         (web/redirect res (redirect-url-fn))
         (handler req res))))))

(defn asset-can-access-repo-wrapper
  "确保请求的repo存在且当前用户有权限访问"                                   ;; TODO: 根据是否是AJAX请求来判断返回格式
  ([handler]
   (asset-can-access-repo-wrapper handler #(web/url-for "index")))
  ([handler redirect-url-fn]
   (fn [^HttpRequest req ^HttpResponse res]
     (let [cur-user (web/current-user req)
           owner-name (.getParam req "user")
           repo-name (.getParam req "repo")
           repo (db/find-repo-by-name-and-user repo-name owner-name)]
       (if (not repo)
         (web/redirect res (redirect-url-fn))
         (if (not (repo-dao/can-access-repo repo cur-user))
           (web/redirect res (redirect-url-fn))
           (handler req res)))))))

(defn asset-can-read-repo-wrapper
  "确保请求的repo存在且当前用户有权限读取"                                   ;; TODO: 根据是否是AJAX请求来判断返回格式
  ([handler]
   (asset-can-read-repo-wrapper handler #(web/url-for "index")))
  ([handler redirect-url-fn]
   (fn [^HttpRequest req ^HttpResponse res]
     (let [cur-user (web/current-user req)
           owner-name (.getParam req "user")
           repo-name (.getParam req "repo")
           repo (db/find-repo-by-name-and-user repo-name owner-name)]
       (if (not repo)
         (web/redirect res (redirect-url-fn))
         (if (not (repo-dao/can-read-repo? repo cur-user))
           (web/redirect res (redirect-url-fn))
           (handler req res)))))))

(defn asset-can-admin-repo-wrapper
  "确保请求的repo存在且当前用户有权限管理"
  ([handler]
   (asset-can-admin-repo-wrapper handler #(web/url-for "index")))
  ([handler redirect-url-fn]
   (fn [^HttpRequest req ^HttpResponse res]
     (let [cur-user (web/current-user req)
           owner-name (.getParam req "user")
           repo-name (.getParam req "repo")
           repo (db/find-repo-by-name-and-user repo-name owner-name)]
       (if (not repo)
         (web/redirect res (redirect-url-fn))
         (if (not (repo-dao/can-admin-repo repo cur-user))
           (web/redirect res (redirect-url-fn))
           (handler req res)))))))

(defn response-wrapper
  ([handler]
   (fn [req res]
     (web/response
       res
       (handler req res)))))

(defn repo-access-required-response-wrapper
  [handler]
  (-> handler
      response-wrapper
      asset-can-access-repo-wrapper))

(defn repo-read-required-response-wrapper
  [handler]
  (-> handler
      response-wrapper
      asset-can-read-repo-wrapper))

(defn repo-admin-required-response-wrapper
  [handler]
  (-> handler
      response-wrapper
      asset-can-admin-repo-wrapper))

(defn login-required-response-wrapper
  [handler]
  (-> handler
      response-wrapper
      asset-login-wrapper))

(defn admin-required-response-wrapper
  [handler]
  (-> handler
      response-wrapper
      asset-admin-wrapper))

(def repo-access-wrapper repo-access-required-response-wrapper)

(def repo-read-wrapper repo-read-required-response-wrapper)

(def repo-admin-wrapper repo-admin-required-response-wrapper)

(def login-wrapper login-required-response-wrapper)

(def admin-wrapper admin-required-response-wrapper)