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

(def find-route http-route-table-match)

(def context context-route)

(def git-routes [(ANY "/:user/:repo/HEAD" [GitHandler "head"] "git_head")
                 (ANY "/:user/:repo/info/refs" [GitHandler "infoRefs"] "git_info_refs")])

;; 暂时匿名http路由有BUG
(def routes (make-route-table
              [(GET "/static/:*path" [StaticFileHandler "handleStaticFile"] "static_route")
               (GET "/" [TestHandler "index"] "index")
               (context "/test" [(GET "/async" [git.TestHandler "async"] "test_async")
                                 (GET "/clojure" [git.TestHandler "clojureHi"] "test_clojure")
                                 (ANY "/hello/:name" [git.TestHandler "hello"] "test_hello")])
               (context "/git" git-routes)
               (ANY "/:*path" [TestHandler "page404"] "page404")]))

