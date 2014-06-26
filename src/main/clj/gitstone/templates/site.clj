(ns gitstone.templates.site
  (:import (java.util Date))
  (:require [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to ordered-list unordered-list image]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [hiccup.page :refer [html5 include-js include-css]]
            [gitstone.layouts :refer :all]
            [gitstone.partials :as partials]
            [gitstone.db :as db]
            [gitstone.web :as web]
            [gitstone.util :as util]
            [gitstone.user-dao :as user-dao]))

(defn index-tmpl
  [req res repos]
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
               [:a {:href (web/url-for "git_view_index" (:owner_name repo) (:name repo))}
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
    nil))

(defn new-repo-tmpl
  [req res]
  (website-layout
    (web/current-user req) "New Git Repo"
    (html
      [:div {:class "row main-content"}
       (ui/horizontal-form
         (html
           (ui/form-group
             (html
               [:label "Repository Name"]
               (ui/form-div
                 (ui/input-field {:type "text" :placeholder "Repository Name" :required "required"} "name"))))
           (ui/form-group
             (html
               [:label "Description"]
               (ui/form-div
                 (ui/input-field {:type "text" :placeholder "Description"} "description"))))
           [:br]
           [:div {:class "checkbox"}
            [:label
             [:input {:type "checkbox" :name "is_private"}]
             [:span {:class "strong"}
              "Private Repository?"]]]
           (ui/form-group
             (ui/form-whole-div
               (ui/success-btn "Create Repository"))))
         (web/url-for "new-repo")
         "POST"
         ""
         {:class "new-repository-form"})])
    "new-repo"))

(defn login-tmpl
  [req res]
  (website-layout
    (web/current-user req) "Login"
    (html
      [:div {:class "row main-content"}
       (ui/horizontal-form
         (html
           (ui/form-group
             (html
               (ui/form-label "Username")
               (ui/form-div
                 (ui/input-field {:type "text" :placeholder "Username" :required "required"} "username"))))
           (ui/form-group
             (html
               (ui/form-label "Password")
               (ui/form-div
                 (ui/input-field {:type "password" :placeholder "Password" :required "required"} "password"))))
           (ui/form-group
             (ui/form-whole-div
               (ui/default-btn "Login"))))
         (web/url-for "login")
         "POST")])
    "login"))

(defn profile-tmpl
  [req res cur-user repos]
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
        [:span (util/format-date (Date. (:created_time cur-user)))]]
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
          (partials/edit-profile-panel cur-user)]]]])))

(defn new-user-tmpl
  [req res]
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
           :POST)]))))

(defn user-list-tmpl
  [req res]
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
                 [:td (util/format-date (Date. (:created_time user)))]
                 [:td (if (user-dao/is-group-account user)
                        "Group"
                        "User")]])]]]])))))