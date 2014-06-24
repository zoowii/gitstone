(ns gitstone.layouts
  (:import (com.zoowii.mvc.http HttpRouter)
           (java.util ArrayList)
           (org.eclipse.jgit.api Git)
           (com.zoowii.gitstone.git GitTreeItem)
           (clojure.lang IPersistentMap))
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-js include-css]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to ordered-list unordered-list image]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [gitstone.db :as db]
            [gitstone.web :as web]
            [gitstone.partials :as partials]
            [gitstone.user-dao :as user-dao]
            [gitstone.repo-dao :as repo-dao]
            [gitstone.util :as util]))

(defn website-layout
  ([cur-user title content]
   (website-layout cur-user title content nil))
  ([cur-user title content cur-module]
   (html5
     [:head
      [:title title]
      [:meta {:charset "utf8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:meta {:name "description" :content "GitStone"}]
      [:meta {:name "author" :content "zoowii"}]
      [:link {:ref "shortcut icon" :href (web/img-url "favicon.jpg")}]
      (include-css
        (web/css-url "bootstrap.min.css")
        (web/css-url "website.css"))
      (include-js
        (web/js-url "jquery.min.js")
        (web/js-url "underscore.min.js")
        (web/js-url "bootstrap.min.js"))]
     [:body
      [:div {:class "blog-masthead"}
       [:div {:class "container"}
        [:nav {:class "blog-nav"}
         (ui/nav-item (web/url-for "index") "Home" (= cur-module nil))
         (ui/nav-item (web/url-for "new-repo-page") "New Repo" (= cur-module "new-repo"))
         (if cur-user
           (ui/nav-item (web/url-for "profile") (:username cur-user) (= cur-module "profile")))
         (if (and cur-user (= (:role cur-user "admin")))
           (ui/nav-item (web/url-for "admin-user-list") "Administration" (= cur-module "admin")))
         (if cur-user
           (ui/nav-item (web/url-for "logout") "Sign Out" (= cur-module "sign-out"))
           (ui/nav-item (web/url-for "login_page" (util/jlist*)) "Sign In" (= cur-module "sign-in")))]]]
      [:div {:class "container main-container"}
       content]
      [:div {:class "blog-footer"}
       [:p
        "Author: "
        [:a {:href "http://github.com/zoowii"}
         "zoowii"]]]])))

(defn view-repo-layout
  "查看git repo的页面布局"
  ([req res username repo-name title
    module content]
   (view-repo-layout req res username repo-name title module content nil nil))
  ([req res username repo-name title
    module content header footer]
   (website-layout
     (web/current-user req) title
     (html
       (when header
         header)
       [:div {:class "row main-content"}
        (partials/view-git-head-patial req username repo-name)
        [:div {:class "row main-content"}
         (partials/view-git-nav-partial req username repo-name module)
         content]]
       (when footer
         footer)))))

(defn view-repo-settings-layout
  [req res username repo-name module content]
  (let [cur-user (web/current-user req)
        repo (db/find-repo-by-name-and-user repo-name username)]
    (view-repo-layout
      req res username repo-name (str "Settings - " repo-name)
      "settings"
      (html
        [:div {:class "row main-content"}
         [:div {:class "col-md-4"}
          (ui/list-group
            [{:active  (= module "options")
              :link    (web/url-for "git-settings-options" username repo-name)
              :content "Options"}
             {:active  (= module "collaborators")
              :link    "#"
              :content "Collaborators"}
             {:active  (= module "service_hooks")
              :link    "#"
              :content "Service Hooks"}
             {:active  (= module "danger_zone")
              :link    (web/url-for "git-settings-danger" username repo-name)
              :content "Danger Zone"}]
            {:class "list-group settings-nav"})]
         [:div {:class "col-md-8"}
          content]]))))
