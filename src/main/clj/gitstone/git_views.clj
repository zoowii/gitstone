(ns gitstone.git-views
  (:import (com.zoowii.gitstone.git GitService GitTreeItem)
           (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (org.eclipse.jgit.api Git)
           (java.util ArrayList)
           (com.zoowii.util FileUtil StringUtil)
           (org.apache.commons.lang StringUtils)
           (com.zoowii.gitstone Settings))
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
            [gitstone.templates.settings :as settings-tmpl]
            [gitstone.templates.view-repo :as view-repo-tmpl]))

(defn- no-access-to-view-repo
  [req res username repo-name]
  (html
    [:pre
     (str "You have no access to view repo " repo-name)]))

(defn- view-path-content
  [req res username repo-name path
   ^Git git cur-branch-name branch-names
   last-commit tree]
  (view-repo-tmpl/view-file-tmpl req res username repo-name path git cur-branch-name branch-names last-commit tree))

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

(defn index
  "浏览repo的首页"
  [req res]
  (let [owner-name (.getParam req "user")
        repo-name (.getParam req "repo")
        repo-path (Settings/getRepoPath owner-name repo-name)
        git-service (GitService/getInstance)
        git (.getGitRepo git-service repo-path)
        branch-names (.getBranchNames git-service git)
        default-branch-name (repo-dao/default-branch-of-repo-by-name owner-name repo-name)
        default-branch-name (if default-branch-name default-branch-name "master")
        cur-branch-name (if (and (not (nil? branch-names)) (seq branch-names))
                          (first branch-names))
        path ""]
    (view-path req res owner-name repo-name git-service git cur-branch-name path)))

(defn view-path-page
  "流量repo的某个path的页面"
  [req res]
  (let [owner-name (.getParam req "user")
        repo-name (.getParam req "repo")
        cur-branch-name (.getParam req "branch")
        path (.getParam req "path")
        path (if (nil? path) "" path)
        path (if (.startsWith path "/")
               (.substring path 1)
               path)
        repo-path (Settings/getRepoPath owner-name repo-name)
        git-service (GitService/getInstance)
        git (.getGitRepo git-service repo-path)
        branch-names (.getBranchNames git-service git)]
    (if (or (empty? branch-names)
            (not (.contains branch-names cur-branch-name)))
      (str "Can't find branch " cur-branch-name " in repo " owner-name "/" repo-name)
      (view-path req res owner-name repo-name git-service git cur-branch-name path))))

(defn create-issue-page
  [req res]
  (let [cur-user (web/current-user req)
        cur-username (db/username-of-user cur-user)
        repo (repo-dao/get-repo-from-req req)
        collaborators (db/find-repo-collaborators-with-owner repo)
        labels (db/get-issue-labels)]
    (issues-tmpl/create-issue-tmpl req res cur-username repo labels collaborators)))

(defn create-issue-handler
  "响应创建issue的请求"
  [req res]
  (let [cur-user (web/current-user req)
        repo-name (.getParam req "repo")
        repo (repo-dao/get-repo-from-req req)
        title (.getPostParam req "title")
        content (.getPostParam req "content")
        assignee-id (.getPostParam req "assignee_id")
        label-id (.getPostParam req "label_id")
        _ (util/clj-debug cur-user repo-name repo title content assignee-id label-id)
        issue (db/new-issue-info (:id cur-user) (:id repo) title content assignee-id label-id)
        _ (db/insert-issue! issue)]
    (web/redirect res (web/url-for "git-issues" (:owner_name repo) (:name repo)))))

(defn view-repo-issues-page
  [req res]
  (let [cur-user (web/current-user req)
        repo (repo-dao/get-repo-from-req req)]
    (issues-tmpl/view-repo-issues-tmpl req res cur-user repo)))

(defn view-repo-settings-danger-zone
  [req res username repo-name]
  (let [repo (db/find-repo-by-name-and-user repo-name username)]
    (settings-tmpl/view-repo-settings-danger-zone-tmpl req res repo)))

(defn view-repo-settings-options
  [req res username repo-name branch-names]
  (let [repo (db/find-repo-by-name-and-user repo-name username)
        branch-names (vec branch-names)]
    (settings-tmpl/view-repo-settings-options-tmpl req res repo branch-names)))

(defn view-repo-collaborators-page
  [req res]
  (let [repo (repo-dao/get-repo-from-req req)
        collaborator-mappings (db/find-repo-collaborators-mapping repo)]
    (settings-tmpl/view-repo-settings-collaborators-tmpl req res repo collaborator-mappings)))

(defn add-repo-collaborator-page
  [req res]
  (let [repo (repo-dao/get-repo-from-req req)]
    (settings-tmpl/add-repo-collaborator-tmpl req res repo)))

(defn add-repo-collaborator-handler
  [req res]
  (let [repo (repo-dao/get-repo-from-req req)
        username (.getPostParam req "username")
        user (db/find-user-by-username username)
        role (.getPostParam req "role")]
    (if (nil? user)
      (web/redirect res (web/url-for "index"))
      (if (db/is-collaborator-of-repo? repo user)
        (web/redirect res (web/url-for "git-add-collaborator" (:owner_name repo) (:name repo)))
        (let [collaborator-mapping (db/new-repo-collaborator-info (:id user) (:id repo) role)
              _ (db/insert-repo-collaborator! collaborator-mapping)]
          (web/redirect res (web/url-for "git-collaborators" (:owner_name repo) (:name repo))))))))

(defn settings-options-page
  "设置Options页面"
  [req res]
  (let [owner-name (.getParam req "user")
        repo-name (.getParam req "repo")
        repo-path (Settings/getRepoPath owner-name repo-name)
        git-service (GitService/getInstance)
        git (.getGitRepo git-service repo-path)
        branch-names (vec (.getBranchNames git-service git))
        branch-names (if (empty? branch-names)
                       (cons "master" branch-names)
                       branch-names)]
    (view-repo-settings-options req res owner-name repo-name branch-names)))

(defn settings-danger-zone-page
  "危险的Settings操作界面"
  [req res]
  (let [owner-name (.getParam req "user")
        repo-name (.getParam req "repo")]
    (view-repo-settings-danger-zone req res owner-name repo-name)))

(defn update-settings-options-handler
  "修改设置的Options"
  [req res]
  (let [owner-name (.getParam req "user")
        repo-name (.getParam req "repo")
        description (.getPostParam req "description")
        default-branch (.getPostParam req "default_branch")
        is-private (.getBoolPostParam req "is_private" true)]
    (repo-dao/update-repo-settings! owner-name repo-name description default-branch is-private)
    (.ajaxSuccess res "update repo settings successfully!")))

(defn delete-repo-handler
  "删除repo"
  [req res]
  (let [owner-name (.getParam req "user")
        repo-name (.getParam req "repo")]
    (repo-dao/del-repo-by-name! owner-name repo-name)
    (.ajaxSuccess res (str "Delete repo " owner-name "/" repo-name " successfully!"))))