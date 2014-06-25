(ns gitstone.routes
  (:import (com.zoowii.mvc.handlers StaticFileHandler)
           (com.zoowii.gitstone.git GitHandler GitViewHandler)
           (com.zoowii.gitstone.handlers SiteHandler))
  (:use any-route.core
        any-route.http)
  (:require [gitstone.views :as views]
            [gitstone.git-views :as git-views]
            [gitstone.view-util :as view-util]))

(defn GET
  [pattern handler route-name]
  (http-route :GET pattern handler route-name))

(defn POST
  [pattern handler route-name]
  (http-route :POST pattern handler route-name))

(defn ANY
  [pattern handler route-name]
  (http-route :ANY pattern handler route-name))

(defmacro defroutes
  "定义路由表"
  [name & body]
  `(def ~name (, make-route-table [~@body])))

(defn url-for
  [routes-table route-name params]
  (apply reverse-in-http-route-table (cons routes-table (cons route-name (vec params)))))

(def find-route http-route-table-match)

(def context context-route)

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

(def git-view-routes [(GET "/branch/:branch" [GitViewHandler "viewPath"] "git_view_branch")
                      (GET "/view/:branch/:*path" [GitViewHandler "viewPath"] "git_view_path")
                      (GET "/settings/options" [GitViewHandler "settingsOptions"] "git-settings-options")
                      (GET "/settings/danger" [GitViewHandler "settingsDangerZone"] "git-settings-danger")
                      (GET "/archive/:branch" [GitViewHandler "archiveRepo"] "git-archive")
                      (POST "/settings/options" [GitViewHandler "updateSettingsOptions"] "update-git-settings-options")
                      (GET "/settings/collaborators" (view-util/repo-admin-wrapper git-views/view-repo-collaborators-page) "git-collaborators")
                      (GET "/settings/collaborators/add" (view-util/repo-admin-wrapper git-views/add-repo-collaborator-page) "git-add-collaborator")
                      (POST "/settings/collaborators/add" git-views/add-repo-collaborator-handler "git-add-collaborator-handler")
                      (POST "/delete" [GitViewHandler "deleteRepo"] "git-delete")
                      (GET "/issues" (view-util/repo-access-wrapper git-views/view-repo-issues-page) "git-issues")
                      (GET "/issues/create" (view-util/repo-access-wrapper git-views/create-issue-page) "git-create-issue")
                      (POST "/issues/create" git-views/create-issue-handler "git-create-issues-handler")
                      (GET "" [GitViewHandler "index"] "git_view_index")])

(def admin-routes [(GET "/users" views/user-list-page "admin-user-list")
                   (GET "/new_user" views/new-user-page "admin-new-user-page")
                   (POST "/new_user" views/admin-new-user "admin-new-user")])

(defn test-page
  [req res]
  (.append res
           "hello from clojure fn"))

(def test-routes [(GET "/fn" test-page "test-fn")
                  (GET "/str" "hi from clojure str" "test-str")
                  (GET "/map" {:status 200 :headers {:Content-Type "text/xml; charset=UTF-8"} :body "hi from clojure map"} "test-map")])

;; 暂时匿名http路由有BUG
(defroutes routes
           (GET "/static/:*path" [StaticFileHandler "handleStaticFile"] "static-file")
           (GET "/" [SiteHandler "index"] "index")
           (GET "/login" [SiteHandler "loginPage"] "login_page")
           (POST "/login" [SiteHandler "login"] "login")
           (ANY "/logout" [SiteHandler "logout"] "logout")
           (GET "/new_repo" [SiteHandler "createRepoPage"] "new-repo-page")
           (GET "/profile" [SiteHandler "profile"] "profile")
           (POST "/change_password" views/edit-profile "edit-profile")
           (POST "/new_repo" [SiteHandler "createRepo"] "new-repo")
           (context "/test" test-routes)
           (context "/admin" admin-routes)
           (context "/git/:user/:repo" git-routes)
           (context "/:user/:repo" git-view-routes)         ;; 因为这个路由的关系,上面路由url中开头的单词都不能作为用户名 TODO
           (ANY "/:*path" [SiteHandler "page404"] "page404"))
