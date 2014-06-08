(ns gitstone.git-views
  (:import (com.zoowii.gitstone.git GitService GitTreeItem)
           (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (org.eclipse.jgit.api Git)
           (java.util ArrayList)
           (com.zoowii.util FileUtil StringUtil ClojureUtil)
           (org.apache.commons.lang StringUtils))
  (:require [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [hiccup.page :refer [html5 include-js include-css]]
            [gitstone.layouts :refer :all]
            [gitstone.web :as web]
            [gitstone.util :as util]
            [gitstone.db :as db]))

(defn- git-clone-url
  [req username repo-name]
  (str (.getFinalHostWithPort req) (web/url-for "git-url" username repo-name)))

(defn- view-git-head-patial
  [req username repo-name]
  (let [repo (db/find-repo-by-name-and-user repo-name username)]
    (html
      [:div {:class "row"}
       [:div {:class "col-sm-8"}
        [:span {:class (if (:is_private repo)
                         "glyphicon glyphicon-lock"
                         "glyphicon glyphicon-folder-open")}]
        [:a {:href  "#"
             :class "font-size-2em"}
         (escape-html username)]
        [:i {:class "slash"} "/"]
        [:a {:href  (web/url-for "git_view_index" username repo-name)
             :class "font-size-2em"}
         (escape-html repo-name)]]
       [:div {:class "col-sm-4"}
        [:button {:type  "button"
                  :class "btn btn-default btn-sm"}
         [:span {:class "glyphicon glyphicon-star"}]
         "Star"]]])))

(defn- view-git-nav-partial
  [req username repo-name]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        cur-user (web/current-user req)]
    (html
      [:ul {:class "nav nav-tabs"}
       [:li {:class "active"}
        [:a {:href "#"} "Code"]]
       [:li
        [:a {:href "#"} "Issues"]]
       [:li
        [:a {:href "#"} "Wiki"]]
       (if (and cur-user (= (:username cur-user) username))
         [:li
          [:button {:class           "btn btn-danger btn-sm delete-repo-btn"
                    :data-owner-name username
                    :data-repo-name  repo-name}
           "Delete"]])])))

(defn- view-git-nav2-partial
  [req username repo-name cur-branch-name branch-names]
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
            [:a {:href "#"}
             b]])]]
       [:a {:type     "button"
            :class    "btn btn-default btn-sm"
            :download (str (escape-html repo-name) ".zip")
            :href     "#"}
        [:span {:class "glyphicon glyphicon-download-alt"}
         "ZIP"]]
       [:hr]
       [:input {:type     "text"
                :class    "form-control"
                :style    "width: 40%; display: inline"
                :value    (git-clone-url req username repo-name)
                :readonly "readonly"}]
       [:hr]])))

(defn- view-git-breadcrumb-partial
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
               _ (ClojureUtil/debug [path-item sub-path-items])
               sub-path (StringUtil/join (jlist sub-path-items) "/")]
           [:li
            (if (< i (dec (count path-items)))
              {}
              {:class "active"})
            [:a {:href (web/url-for "git_view_path" username repo-name cur-branch-name sub-path)}
             (str "&nbsp;" (escape-html path-item))]]))
       (when-not (seq path-items)
         [:li])])))

(defn- view-git-last-commit-partial
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

(defn- view-path-content
  "如果这个git repo不为空,则显示文件夹列表或者文件内容"
  [req res username repo-name
   ^GitService git-service ^Git git
   cur-branch-name path
   branch-names last-commit tree]
  (website-layout
    (web/current-user req) (str repo-name " - " path)
    (html
      (when-not (.isTree tree)
        (include-js (js-url "src-min/ace.js")))
      [:div {:class "row main-content"}
       (view-git-head-patial req username repo-name)
       [:div {:class "row main-content"}
        (view-git-nav-partial req username repo-name)
        [:div
         (view-git-nav2-partial req username repo-name cur-branch-name branch-names)
         [:div
          (view-git-breadcrumb-partial req username repo-name cur-branch-name path)
          [:div
           [:div {:class "panel panel-default"}
            [:div {:class "panel-heading"}
             (view-git-last-commit-partial req username repo-name last-commit)]
            [:div {:class "panel-body"}
             (when-not (.isTree tree)
               (let [stream (.getBlob git-service git (.getObjectId tree))
                     preview-text (FileUtil/tryParseStreamToString stream)]
                 [:div (if preview-text
                         {:id "code-preview-area" :style (str "width: 100%; min-height: " (int (* 1.5 (StringUtils/countMatches preview-text "\n"))) "em")}
                         {:id "code-preview-area"})
                  (if preview-text
                    (escape-html preview-text)
                    "Can't preview this file yet")]))]
            (if (.isTree tree)
              [:ul {:class "list-group"}
               (for [item (.getItems tree)]
                 [:li {:class "list-group-item"}
                  [:span {:class (if (.isTree item)
                                   "glyphicon glyphicon-book"
                                   "glyphicon glyphicon-list-alt")}]
                  [:a {:href (web/url-for "git_view_path" username repo-name cur-branch-name (.getPath item))}
                   (.getName item)]])])]]]]]]
      (when-not (.isTree tree)
        (include-js (js-url "preview_git_file.js"))))))

(defn- display-empty-repo
  "显示空git repo"
  [req res username repo-name]
  (website-layout
    (web/current-user req) (str "GitStone " username "/" repo-name)
    (html
      [:div {:class "row main-content"}
       [:pre
        (str "This repository is empty. You can start with")
        [:br]
        (str "git clone " (git-clone-url req username repo-name))]])))

(defn view-path
  "浏览git repo某个分支的某个路径"
  [^HttpRequest req ^HttpResponse res
   username repo-name
   ^GitService git-service ^Git git
   cur-branch-name path]
  (let [branch-names (vec (.getBranchNames git-service git))
        last-commit (ggit/last-commit git-service git cur-branch-name)
        ^GitTreeItem tree (ggit/object-in-path git-service git cur-branch-name path)]
    (if cur-branch-name
      (view-path-content req res username repo-name git-service git cur-branch-name path branch-names last-commit tree)
      (display-empty-repo req res username repo-name))))
