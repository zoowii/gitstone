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
            [gitstone.ui :as ui]))

(defn index-page
  [^HttpRequest req ^HttpResponse res]
  (let [cur-user (web/current-user req)
        limit (.getIntParam req "limit" 10)
        offset (.getIntParam req "offset" 0)
        limit (if (pos? limit) limit 100)
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

(defn- edit-profile-panel
  [cur-user]
  (html
    (ui/horizontal-form
      (html
        (ui/simple-form-group
          (ui/form-label "New Password")
          (ui/form-div
            (ui/input-field {:required "required"
                             :type     "password"}
                            "password")))
        (ui/simple-form-group
          (ui/form-whole-div
            (ui/danger-btn "Change Password"))))
      (web/url-for "edit-profile")
      :POST)))

(defn profile
  [^HttpRequest req ^HttpResponse res]
  (let [cur-user (web/current-user req)
        repos (db/find-repos-of-user cur-user 0 100)]
    (website-layout
      cur-user "profile"
      (html
        [:div {:class "row main-content"}
         [:div {:class "col-sm-4"}
          [:div {:class "account-image"}
           [:img {:src   (:image cur-user)
                  :alt   "..."
                  :class "img-thumbnail"}]]
          [:h4 (:username cur-user)]
          [:span {:class "glyphicon glyphicon-user"}]
          [:span "Joined @"]
          [:span (format-date (Date. (:created_time cur-user)))]]
         [:div {:class "col-sm-8"}
          [:ul {:class "nav nav-tabs"}
           [:li {:class "active"}
            [:a {:href        "#repositories-area"
                 :data-toggle "tab"}
             "Repositories"]]
           [:li
            [:a {:href        "#profile"
                 :data-toggle "tab"}
             "Organizations"]]
           [:li
            [:a {:href        "#messages"
                 :data-toggle "tab"}
             "Public Activities"]]
           [:li
            [:a {:href        "#edit-profile"
                 :data-toggle "tab"}
             "Edit Profile"]]]
          [:div {:class "tab-content"}
           [:div {:class "tab-pane active"
                  :id    "repositories-area"}
            [:ul {:class "list-group"}
             (for [repo repos]
               [:li {:class "list-group-item"}
                [:span {:class "badge"} "Star 0"]
                [:span {:class "badge"} "Fork 0"]
                [:a {:href (web/url-for "git_view_index" (:owner_name repo) (:name repo))}
                 [:h4 (escape-html (:name repo))]]
                [:pre (escape-html (:description repo))]])]]
           [:div {:class "tab-pane"
                  :id    "profile"}
            "..."]
           [:div {:class "tab-pane"
                  :id    "messages"}
            "..."]
           [:div {:class "tab-pane"
                  :id    "edit-profile"}
            (edit-profile-panel cur-user)]]]]))))

(defn new-user-page
  [req res]
  (web/response
    res
    (website-layout
      (web/current-user req) "new-user-page"
      (if (not (user-dao/is-admin-user-req req))
        (web/redirect res (web/url-for "login_page"))
        (html
          [:div {:class "row main-content"}
           [:h4 "New User"]
           (ui/horizontal-form
             (html
               (ui/simple-form-group
                 (ui/form-label "User Name")
                 (ui/input-field {:type     "text"
                                  :required "required"}
                                 "username"))
               (ui/simple-form-group
                 (ui/form-label "Email")
                 (ui/input-field {:type     "email"
                                  :required "required"}
                                 "email"))
               (ui/simple-form-group
                 (ui/form-label "Password")
                 (ui/input-field {:type     "password"
                                  :required "required"}
                                 "password"))
               (ui/form-whole-div
                 (ui/default-btn "Create User"
                                 {:type "submit"})))
             (web/url-for "admin-new-user")
             :POST)])))))

(defn user-list-page
  [req res]
  (web/response
    res
    (website-layout
      (web/current-user req) "user-list"
      (if (not (user-dao/is-admin-user-req req))
        (web/redirect res (web/url-for "login_page"))
        (let [users (db/find-users 0 100)]
          (html
            [:div {:class "row main-content"}
             [:div {:class "panel panel-primary"}
              [:div {:class "panel-heading"}
               "Managing Users"]
              [:div {:class "panel-body"}
               [:div {:class "btn-group"}
                (ui/default-link "New User" (web/url-for "admin-new-user-page"))
                (ui/default-btn "New Group")]]
              [:table {:class "table table-bordered table-stripped"}
               [:thead
                [:th "ID"]
                [:th "Name"]
                [:th "Role"]
                [:th "Join Time"]
                [:th "Type"]]
               [:tbody
                (for [user users]
                  [:tr
                   [:td (:id user)]
                   [:td (:username user)]
                   [:td (:role user)]
                   [:td (format-date (Date. (:created_time user)))]
                   [:td (if (user-dao/is-group-account user)
                          "Group"
                          "User")]])]]]]))))))