(ns gitstone.views
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse))
  (:require [hiccup.core :refer [html]]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [gitstone.layouts :refer :all]
            [gitstone.ui :refer :all]
            [gitstone.db :as db]
            [gitstone.user-dao :as user-dao]
            [gitstone.web :as web]
            [gitstone.util :refer [uuid now-timestamp]]))

(defn index-page
  [^HttpRequest req ^HttpResponse res]
  (let [cur-user (web/current-user req)
        limit (.getIntParam req "limit" 10)
        offset (.getIntParam req "offset" 0)
        limit (if (pos? limit) limit 10)
        offset (if (>= offset 0) offset 0)
        repos (db/find-repos-of-user cur-user offset limit)]
    (website-layout
      (web/current-user req) "GitStone"
      (html
        [:div {:class "row main-content"}
         [:div {:class "col-sm-8 blog-main"}
          [:ul {:class "nav nav-tabs"}
           [:li {:class "active"}
            [:a {:href "#"}
             "Activities"]]
           [:li
            [:a {:href "#"}
             "Pull Requests"]]
           [:li
            [:a {:href "#"}
             "Issues"]]]]
         [:div {:class "col-sm-3 col-sm-offset-1 blog-sidebar"}
          [:div {:class "sidebar-module sidebar-module-inset"}
           [:h4 "About"]
           [:p "GitStone, another clone of Github"]]
          [:div {:class "sidebar-module"}
           [:h4
            [:a {:class "btn btn-xs btn-success pull-right"
                 :href  (web/url-for "new-repo")}
             "New Repo"]
            "Your Repositories"]
           [:ol {:class "list-unstyled"}
            (if (empty? repos)
              [:li "Empty Repositories"]
              (for [repo repos]
                [:li
                 [:a {:href (web/url-for "git_view_index" (:username cur-user) (:name repo))}
                  (:name repo)]]))
            [:li
             [:a {:href "#"}
              "..."]]]]
          [:div {:class "sidebar-module"}
           [:h4 "Elsewhere"]
           [:ol {:class "list-unstyled"}
            [:li
             [:a {:href "https://github.com/zoowii/gitstone"}
              "Github"]]
            [:li
             [:a {:href "http://zoowii.com"}
              "Author: @zoowii"]]]]]])
      nil)))

(defn create-repo
  [^HttpRequest req ^HttpResponse res]
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
  ;; login required. TODO
  [^HttpRequest req ^HttpResponse res]
  (website-layout
    (web/current-user req) "New Git Repo"
    (html
      [:div {:class "row main-content"}
       (horizontal-form
         (html
           (form-group
             (html
               [:label "Repository Name"]
               (form-div
                 (input-field {:type "text" :placeholder "Repository Name" :required "required"} "name"))))
           (form-group
             (html
               [:label "Description"]
               (form-div
                 (input-field {:type "text" :placeholder "Description"} "description"))))
           [:br]
           [:div {:class "checkbox"}
            [:label
             [:input {:type "checkbox" :name "is_private"}]
             [:span {:class "strong"}
              "Private Repository?"]]]
           (form-group
             (form-whole-div
               (success-btn "Create Repository"))))
         (web/url-for "new-repo")
         "POST"
         ""
         {:class "new-repository-form"})])
    "new-repo"))


(defn login-page
  [^HttpRequest req ^HttpResponse res]
  (website-layout
    (web/current-user req) "Login"
    (html
      [:div {:class "row main-content"}
       (horizontal-form
         (html
           (form-group
             (html
               (form-label "Username")
               (form-div
                 (input-field {:type "text" :placeholder "Username" :required "required"} "username"))))
           (form-group
             (html
               (form-label "Password")
               (form-div
                 (input-field {:type "password" :placeholder "Password" :required "required"} "password"))))
           (form-group
             (form-whole-div
               (default-btn "Login"))))
         (web/url-for "login")
         "POST")])
    "login"))

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