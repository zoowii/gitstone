(ns gitstone.routes
  (:import (com.zoowii.mvc.handlers StaticFileHandler TestHandler)
           (git GitHandler))
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

(def find-route http-route-table-match)

(def context context-route)

(def git-routes [(GET "/HEAD" [GitHandler "head"] "git_head")
                 (GET "/info/refs" [GitHandler "infoRefs"] "git_info_refs")
                 (GET "/objects/info/packs" [GitHandler "infoPacks"] "git_info_packs")
                 (GET "/objects/info/:path" [GitHandler "textInfo"] "git_text_info")
                 (GET "/objects/pack/pack-:path.pack" [GitHandler "packFile"] "git_pack_file")
                 (GET "/objects/pack/pack-:path.idx" [GitHandler "idxFile"] "git_idx_file")
                 (GET "/objects/info/:*path" [GitHandler "looseObject"] "git_loose_object")
                 (POST "/git-upload-pack" [GitHandler "uploadPack"] "git_upload_pack")
                 (POST "/git-receive-pack" [GitHandler "receivePack"] "git_receive_pack")])

;; 暂时匿名http路由有BUG
(defroutes routes
           (GET "/static/:*path" [StaticFileHandler "handleStaticFile"] "static_route")
           (GET "/" [TestHandler "index"] "index")
           (context "/test" [(GET "/async" [git.TestHandler "async"] "test_async")
                             (GET "/clojure" [git.TestHandler "clojureHi"] "test_clojure")
                             (ANY "/hello/:name" [git.TestHandler "hello"] "test_hello")])
           (context "/git/:user/:repo" git-routes)
           (ANY "/:*path" [TestHandler "page404"] "page404"))
