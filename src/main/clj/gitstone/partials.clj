(ns gitstone.partials
  (:import (com.zoowii.mvc.http HttpRouter HttpRequest)
           (java.util ArrayList Date)
           (com.zoowii.util ClojureUtil StringUtil))
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-js include-css]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to ordered-list unordered-list image]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [gitstone.db :as db]
            [gitstone.web :as web]
            [gitstone.user-dao :as user-dao]
            [gitstone.repo-dao :as repo-dao]
            [gitstone.util :as util]))


(defn git-clone-url
  [^HttpRequest req username repo-name]
  (str (.getFinalHostWithPort req) (web/url-for "git-url" username repo-name)))

(defn view-git-head-patial
  [req username repo-name]
  (let [repo (db/find-repo-by-name-and-user repo-name username)]
    (html
      [:div {:class "row"}
       [:div {:class "col-sm-8"}
        [:span {:class (if (repo-dao/is-repo-private repo)
                         "glyphicon glyphicon-lock"
                         "glyphicon glyphicon-folder-open")}]
        [:a {:href  "#"
             :class "font-size-2em"}
         (str "&nbsp;" (escape-html username) "&nbsp;")]
        [:i {:class "slash"} "/"]
        [:a {:href  (web/url-for "git_view_index" username repo-name)
             :class "font-size-2em"}
         (str "&nbsp;" (escape-html repo-name)) "&nbsp;"]]
       [:div {:class "col-sm-4"}
        [:button {:type  "button"
                  :class "btn btn-default btn-sm"}
         [:span {:class "glyphicon glyphicon-star"}]
         "Star"]]])))

(defn view-git-nav-partial
  [req username repo-name active-module]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        cur-user (web/current-user req)]
    (html
      [:ul {:class "nav nav-tabs"}
       [:li (if (= active-module "code")
              {:class "active"}
              {})
        [:a {:href (web/url-for "git_view_index" username repo-name)} "Code"]]
       [:li (if (= active-module "commits")
              {:class "active"}
              {})
        [:a {:href "#"} "Commits"]]
       [:li (if (= active-module "issues")
              {:class "active"}
              {})
        (link-to (web/url-for "git-issues" username repo-name) "Issues")]
       [:li (if (= active-module "pull_requests")
              {:class "active"}
              {})
        (link-to "#" "Pull Requests")]
       [:li (if (= active-module "wiki")
              {:class "active"}
              {})
        [:a {:href "#"} "Wiki"]]
       [:li (if (= active-module "network")
              {:class "active"}
              {})
        (link-to "#" "Network")]
       [:li (if (= active-module "settings")
              {:class "active"}
              {})
        (link-to (web/url-for "git-settings-options" username repo-name) "Settings")]])))

(defn view-git-nav2-partial
  [req username repo-name cur-branch-name branch-names path]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        cur-user (web/current-user req)]
    (html
      [:div {:style "padding-top: 20px;"}
       [:div {:class "btn-group"}
        [:button {:type  "button"
                  :class "btn btn-default btn-sm"}
         cur-branch-name]
        [:button {:type        "button"
                  :class       "btn btn-default btn-sm dropdown-toggle"
                  :data-toggle "dropdown"}
         [:span {:class "caret"}]
         [:span {:class "sr-only"} "Toggle Down"]]
        [:ul {:class "dropdown-menu"
              :role  "menu"}
         (for [b branch-names]
           [:li
            [:a {:href (if (pos? (count path))
                         (web/url-for "git_view_path" username repo-name b path)
                         (web/url-for "git_view_branch" username repo-name b))}
             b]])]]
       [:a {:type     "button"
            :class    "btn btn-default btn-sm"
            :download (str (escape-html repo-name) "." (escape-html cur-branch-name) ".zip")
            :href     (web/url-for "git-archive" username repo-name cur-branch-name)}
        [:span {:class "glyphicon glyphicon-download-alt"}
         "ZIP"]]
       [:hr]
       [:input {:type     "text"
                :class    "form-control"
                :style    "width: 40%; display: inline"
                :value    (git-clone-url req username repo-name)
                :readonly "readonly"}]
       [:hr]])))

(defn view-git-breadcrumb-partial
  [req username repo-name cur-branch-name path]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        path-items (vec (StringUtil/splitPath path))]
    (html
      [:ol {:class "breadcrumb"}
       [:li
        [:a {:class "active"
             :href  (web/url-for "git_view_index" username repo-name)}
         (escape-html repo-name)]]
       (for [i (range (count path-items))]
         (let [path-item (get path-items i)
               sub-path-items (.subList path-items 0 (inc i))
               _ (ClojureUtil/debug [i path-item sub-path-items path-items])
               sub-path (StringUtil/join (util/jlist sub-path-items) "/")]
           (if (< i (dec (count path-items)))
             [:li
              [:a {:href (web/url-for "git_view_path" username repo-name cur-branch-name sub-path)}
               (str "&nbsp;" (escape-html path-item))]]
             [:li {:class "active"}
              (escape-html path-item)])))
       (when-not (seq path-items)
         [:li])])))

(defn view-git-last-commit-partial
  [req username repo-name last-commit]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        cur-user (web/current-user req)]
    (if last-commit
      (html
        [:span (ggit/commit-author-name last-commit)]
        [:span (ggit/commit-time-str last-commit)]
        [:span (ggit/commit-short-msg last-commit)]
        [:span "Last Commit"]
        [:span (ggit/commit-id last-commit)]))))

(defn issue-list-with-panel
  "带有管理面板的Issues列表"
  [req res cur-user repo]
  (let [issues (db/find-issues-by-repo repo)]
    (html
      [:div {:class "row"
             :style "height: 30px"}]
      [:div {:class "row"}
       [:a {:class "btn btn-mn btn-default"
            :href  (web/url-for "git-create-issue" (:owner_name repo) (:name repo))}
        "Create Issue"]]
      [:div {:class "row"}
       [:span "Filters: "]
       [:a {:href "#"}
        [:span {:class "label label-primary"}
         "Open"]]
       [:a {:href "#"}
        [:span {:class "label label-default"}
         "Closed"]]]
      [:div {:class "row"}
       [:table {:class "table table-bordered table-stripped"}
        [:thead
         [:th "Title"]
         [:th "Assigned To"]
         [:th "Status"]
         [:th "Created"]
         [:th "Updated"]]
        [:tbody
         (for [issue issues]
           [:tr
            [:td (:title issue)]
            [:td (if-let [assignee (db/get-assignee-of-issue issue)]
                   (:username assignee))]
            [:td (:status issue)]
            [:td (util/format-date (Date. (:created_time issue))
                                   "yyyy-MM-dd HH:mm:ss"
                                   util/zh-cn-time-zone)]
            [:td (util/format-date (Date. (:last_updated_time issue))
                                   "yyyy-MM-dd HH:mm:ss"
                                   util/zh-cn-time-zone)]])]]])))

(defn edit-profile-panel
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