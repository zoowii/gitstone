(ns gitstone.views
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse)
           (java.util Date)
           (com.zoowii.util ClojureUtil))
  (:require [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [gitstone.layouts :refer :all]
            [gitstone.ui :refer :all]
            [gitstone.db :as db]
            [gitstone.user-dao :as user-dao]
            [gitstone.web :as web]
            [gitstone.util :refer [uuid now-timestamp format-date]]
            [gitstone.ui :as ui]
            [gitstone.templates.site :as site-tmpl]))

(defn index-page
  [req res]
  (let [cur-user (web/current-user req)
        limit (.getIntParam req "limit" 10)
        offset (.getIntParam req "offset" 0)
        limit (if (pos? limit) limit 100)
        offset (if (>= offset 0) offset 0)
        repos (db/find-repos-user-can-access cur-user offset limit)]
    (site-tmpl/index-tmpl req res repos)))

(defn create-repo
  [req res]
  (let [user (web/current-user req)
        name (.getPostParam req "name")
        description (.getPostParam req "description")
        is-private (= (.getPostParam req "is_private") "on")
        repo (db/find-repo-by-name-and-user name user)]
    (if (or repo
            (not (db/check-repo-name name)))
      (do
        (web/redirect res (web/url-for "new-repo-page")))
      (let [repo {:id                     (uuid)
                  :version                1
                  :created_time           (now-timestamp)
                  :name                   name
                  :is_private             is-private
                  :description            description
                  :default_branch         "master"
                  :last_updated_time      (now-timestamp)
                  :owner_name             (:username user)
                  :last_active_time       (now-timestamp)
                  :parent_user_name       nil
                  :parent_repository_name nil}
            _ (db/create-repo! repo)]                       ;; 在文件系统中创建真实的git目录
        (web/redirect res (web/url-for "git_view_index" (:username user) (:name repo)))))))

(defn new-repo-page
  [req res]
  (site-tmpl/new-repo-tmpl req res))


(defn login-page
  [req res]
  (site-tmpl/login-tmpl req res))

(defn login
  [^HttpRequest req ^HttpResponse res]
  (let [username (.getPostParam req "username")
        password (.getPostParam req "password")
        user (db/find-user-by-username username)]
    (if (or (nil? user)
            (not (user-dao/check-user-password user password)))
      (do
        (logging/info "login error")
        (logging/info username password)
        (web/redirect res (web/url-for "login_page")))
      (do
        (web/login-as-user req user)
        (web/redirect res (web/url-for "index"))))))

(defn admin-new-user
  [req res]
  (let [cur-user (web/current-user req)
        username (.getPostParam req "username")
        password (.getPostParam req "password")
        email (.getPostParam req "email")]
    (if (nil? cur-user)
      (web/redirect res (web/url-for "login_page"))
      (if (or (not (user-dao/check-username-available username))
              (not (user-dao/check-email-available email))
              (< (count username) 4)
              (< (count password) 4))
        (web/redirect res (web/url-for "admin-new-user-page"))
        (let [user (db/new-user-info username email password)]
          (db/create-user! user)
          (web/redirect res (web/url-for "admin-user-list")))))))

(defn edit-profile
  [req res]
  (let [cur-user (web/current-user req)
        password (.getPostParam req "password")]
    (if (nil? cur-user)
      (web/redirect res (web/url-for "login_page"))
      (let [new-password (user-dao/encrypt-password password (:salt cur-user))]
        (db/update-user-by-id! {:password new-password}
                               (:id cur-user))
        (user-dao/add-user-change-log! cur-user)
        (web/redirect res (web/url-for "profile"))))))

(defn logout
  [req res]
  (.clearSession req)
  (web/redirect res (web/url-for "index")))

(defn not-found-page
  [req res]
  (html
    [:head
     [:meta {:charset "UTF-8"}]]
    [:body
     [:h1 "404~~~~你家人知道吗?"]]))

(defn profile
  [req res]
  (let [cur-user (web/current-user req)
        repos (db/find-repos-of-user cur-user 0 100)]
    (site-tmpl/profile-tmpl req res cur-user repos)))

(defn new-user-page
  [req res]
  (site-tmpl/new-user-tmpl req res))

(defn user-list-page
  [req res]
  (site-tmpl/user-list-tmpl req res))