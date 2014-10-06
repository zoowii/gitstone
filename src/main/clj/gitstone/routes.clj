(ns gitstone.routes
  (:import (com.zoowii.mvc.handlers StaticFileHandler)
           (com.zoowii.gitstone.git GitHandler GitViewHandler))
  (:use any-route.core
        any-route.http)
  (:require [gitstone.views :as views]
            [gitstone.git-views :as git-views]
            [gitstone.view-util :as view-util]))

(def git-routes [(GET "" [GitHandler "cloneDummy"] "git-url")
                 (GET "/HEAD" [GitHandler "head"] "git_head")
                 (GET "/info/refs" [GitHandler "infoRefs"] "git_info_refs")
                 (GET "/objects/info/packs" [GitHandler "infoPacks"] "git_info_packs")
                 (GET "/objects/info/:path" [GitHandler "textInfo"] "git_text_info")
                 (GET "/objects/pack/pack-:path.pack" [GitHandler "packFile"] "git_pack_file")
                 (GET "/objects/pack/pack-:path.idx" [GitHandler "idxFile"] "git_idx_file")
                 (GET "/objects/info/:*path" [GitHandler "looseObject"] "git_loose_object")
                 (POST "/git-upload-pack" [GitHandler "uploadPack"] "git_upload_pack")
                 (POST "/git-receive-pack" [GitHandler "receivePack"] "git_receive_pack")])

(def git-view-routes [(GET "/branch/:branch" (view-util/repo-read-wrapper git-views/view-path-page) "git_view_branch")
                      (GET "/view/:branch/:*path" (view-util/repo-read-wrapper git-views/view-path-page) "git_view_path")
                      (GET "/settings/options" (view-util/repo-admin-wrapper git-views/settings-options-page) "git-settings-options")
                      (GET "/settings/danger" (view-util/repo-admin-wrapper git-views/settings-danger-zone-page) "git-settings-danger")
                      (GET "/archive/:branch" [GitViewHandler "archiveRepo"] "git-archive")
                      (POST "/settings/options" (view-util/repo-admin-wrapper git-views/update-settings-options-handler) "update-git-settings-options")
                      (GET "/settings/collaborators" (view-util/repo-admin-wrapper git-views/view-repo-collaborators-page) "git-collaborators")
                      (GET "/settings/collaborators/add" (view-util/repo-admin-wrapper git-views/add-repo-collaborator-page) "git-add-collaborator")
                      (POST "/settings/collaborators/add" git-views/add-repo-collaborator-handler "git-add-collaborator-handler")
                      (POST "/delete" (view-util/repo-admin-wrapper git-views/delete-repo-handler) "git-delete")
                      (GET "/issues" (view-util/repo-access-wrapper git-views/view-repo-issues-page) "git-issues")
                      (GET "/issues/create" (view-util/repo-access-wrapper git-views/create-issue-page) "git-create-issue")
                      (POST "/issues/create" git-views/create-issue-handler "git-create-issues-handler")
                      (GET "" (view-util/repo-read-wrapper git-views/index) "git_view_index")])

(def admin-routes [(GET "/users" (view-util/admin-wrapper views/user-list-page) "admin-user-list")
                   (GET "/new_user" (view-util/admin-wrapper views/new-user-page) "admin-new-user-page")
                   (POST "/new_user" (view-util/admin-required-response-wrapper views/admin-new-user) "admin-new-user")])

(defn test-page
  [req res]
  (.append res
           "hello from clojure fn"))

(def test-routes [(GET "/fn" test-page)
                  (GET "/str" "hi from clojure str")
                  (GET "/map" {:status 200 :headers {:Content-Type "text/xml; charset=UTF-8"} :body "hi from clojure map"})])

(defroutes routes
           (GET "/static/:*path" [StaticFileHandler "handleStaticFile"] "static-file")
           (GET "/" (view-util/login-wrapper views/index-page) "index")
           (GET "/login" (view-util/response-wrapper views/login-page) "login_page")
           (POST "/login" views/login "login")
           (ANY "/logout" views/logout "logout")
           (GET "/new_repo" (view-util/login-wrapper views/new-repo-page) "new-repo-page")
           (GET "/profile" (view-util/login-wrapper views/profile) "profile")
           (POST "/change_password" (view-util/login-wrapper views/edit-profile) "edit-profile")
           (POST "/new_repo" (view-util/login-wrapper views/create-repo) "new-repo")
           (context "/test" test-routes)
           (context "/admin" admin-routes)
           (context "/git/:user/:repo" git-routes)
           (context "/:user/:repo" git-view-routes)         ;; 因为这个路由的关系,上面路由url中开头的单词都不能作为用户名 TODO
           (ANY "/:*path" (view-util/response-wrapper views/not-found-page) "page404"))

(def interceptors [])
(def middlewares [])