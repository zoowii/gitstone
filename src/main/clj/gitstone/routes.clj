(ns gitstone.routes
  (:import (com.zoowii.mvc.handlers StaticFileHandler)
           (com.zoowii.gitstone.git GitHandler GitViewHandler)
           (com.zoowii.gitstone.handlers SiteHandler))
  (:use any-route.core
        any-route.http))

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

(def git-view-routes [(GET "/tree/:branch" [GitViewHandler "viewPath"] "git_view_branch")
                      (GET "/tree/view/:branch/:*path" [GitViewHandler "viewPath"] "git_view_path")
                      (GET "" [GitViewHandler "index"] "git_view_index")])

;; 暂时匿名http路由有BUG
(defroutes routes
           (GET "/static/:*path" [StaticFileHandler "handleStaticFile"] "static-file")
           (GET "/" [SiteHandler "index"] "index")
           (GET "/login" [SiteHandler "loginPage"] "login_page")
           (POST "/login" [SiteHandler "login"] "login")
           (ANY "/logout" [SiteHandler "logout"] "logout")
           (GET "/new_repo" [SiteHandler "createRepoPage"] "new-repo-page")
           (POST "/new_repo" [SiteHandler "createRepo"] "new-repo")
           (context "/git/:user/:repo" git-routes)
           (context "/:user/:repo" git-view-routes)         ;; 因为这个路由的关系,上面路由url中开头的单词都不能作为用户名 TODO
           (ANY "/:*path" [SiteHandler "page404"] "page404"))
