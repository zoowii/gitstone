(ns gitstone.git-views
  (:import (com.zoowii.gitstone.git GitService GitTreeItem)
           (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (org.eclipse.jgit.api Git)
           (java.util ArrayList)
           (com.zoowii.util FileUtil StringUtil)
           (org.apache.commons.lang StringUtils))
  (:require [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to ordered-list unordered-list image]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [hiccup.page :refer [html5 include-js include-css]]
            [gitstone.layouts :refer :all]
            [gitstone.web :as web]
            [gitstone.util :as util]
            [gitstone.db :as db]
            [gitstone.partials :as partials]
            [gitstone.repo-dao :as repo-dao]
            [gitstone.view-util :as view-util]
            [gitstone.templates.issues :as issues-tmpl]
            [gitstone.templates.settings :as settings-tmpl]))

(defn- no-access-to-view-repo
  [req res username repo-name]
  (html
    [:pre
     (str "You have no access to view repo " repo-name)]))

(defn- view-path-content
  [req res username repo-name path
   ^Git git cur-branch-name branch-names
   last-commit tree]
  (view-repo-layout
    req res username repo-name (str repo-name " " path)
    "code"
    (html
      [:div
       (partials/view-git-nav2-partial req username repo-name cur-branch-name branch-names path)
       [:div
        (partials/view-git-breadcrumb-partial req username repo-name cur-branch-name path)
        [:div
         [:div {:class "panel panel-default"}
          [:div {:class "panel-heading"}
           (partials/view-git-last-commit-partial req username repo-name last-commit)]
          [:div {:class "panel-body"}
           (when-not (.isTree tree)
             (let [stream (.getBlob (GitService/getInstance) git (.getObjectId tree))
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
                 (.getName item)]])])]]]])
    (when-not (.isTree tree)
      (include-js (web/js-url "src-min/ace.js")))
    (when-not (.isTree tree)
      (include-js (web/js-url "preview_git_file.js")))))

(defn- display-empty-repo
  "显示空git repo"
  [req res username repo-name]
  (view-repo-layout
    req res username repo-name (str "GitStone " username "/" repo-name)
    "code"
    (html
      [:div {:class "row main-content"}
       [:pre
        (str "This repository is empty. You can start with")
        [:br]
        (str "git clone " (partials/git-clone-url req username repo-name))]])))

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
      (view-path-content req res username repo-name path git cur-branch-name branch-names last-commit tree)
      (display-empty-repo req res username repo-name))))

(defn create-issue-page-handler
  [req res]
  (let [cur-user (web/current-user req)
        cur-username (db/username-of-user cur-user)
        repo-name (.getParam req "repo")
        repo (db/find-repo-by-name-and-user repo-name cur-user)
        collaborators (db/find-repo-collaborators-with-owner repo)
        labels (db/get-issue-labels)]
    (issues-tmpl/create-issue-tmpl req res cur-username repo labels collaborators)))

(def create-issue-page
  (-> create-issue-page-handler
      view-util/response-wrapper
      view-util/asset-can-access-repo-wrapper))

(defn create-issue-handler
  "响应创建issue的请求"
  [req res]
  (let [cur-user (web/current-user req)
        cur-username (db/username-of-user cur-user)
        repo-name (.getParam req "repo")
        repo (db/find-repo-by-name-and-user repo-name cur-user)
        title (.getPostParam req "title")
        content (.getPostParam req "content")
        assignee-id (.getPostParam req "assignee_id")
        label-id (.getPostParam req "label_id")
        _ (util/clj-debug cur-user repo-name repo title content assignee-id label-id)
        issue (db/new-issue-info (:id cur-user) (:id repo) title content assignee-id label-id)
        _ (db/insert-issue! issue)]
    (web/redirect res (web/url-for "git-issues" (:owner_name repo) (:name repo)))))

(defn view-repo-issues-handler
  [req res]
  (let [cur-user (web/current-user req)
        repo-name (.getParam req "repo")
        repo (db/find-repo-by-name-and-user repo-name cur-user)]
    (issues-tmpl/view-repo-issues-tmpl req res cur-user repo)))

(def view-repo-issues-page
  (-> view-repo-issues-handler
      view-util/response-wrapper
      view-util/asset-can-access-repo-wrapper))

(defn view-repo-settings-danger-zone
  [req res username repo-name]
  (let [repo (db/find-repo-by-name-and-user repo-name username)]
    (settings-tmpl/view-repo-settings-danger-zone-tmpl req res repo)))

(defn view-repo-settings-options
  [req res username repo-name branch-names]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        branch-names (vec branch-names)]
    (settings-tmpl/view-repo-settings-options-tmpl req res repo branch-names)))
