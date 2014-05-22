(ns any-route.test
  (:use any-route.core
        any-route.http))

;; any-routes的测试代码

(def test-url "/user/123/view/any-note/abc/def")
(def test-route "/user/:id/view/:project/:*path")

(println ((make-route test-route) test-url))
(println (make-un-route test-route 123 "dd" "ddfa/dd"))

(def rtbl (make-route-table
            [
              (route "/test/:id/update" "test_handler" "test")
              (context-route "/user"
                             [
                               (route "/:id/view/:project/:*path" "view_user_handler" "view_user")
                               ])
              ]))
(println rtbl)
(println (find-route-in-route-table rtbl "view_user"))

(println (route-table-match rtbl test-url))
(println (reverse-in-route-table rtbl "view_user" "433" "test-project" "github.com/zoowii"))

(def http-rtbl (make-route-table
                 [
                   (http-route :GET "/test/:id/update" "test_handler" "test")
                   (context-route "/user"
                                  [
                                    (http-route :GET "/:id/view/:project/:*path" "view_user_handler" "view_user")
                                    (http-route :POST "/:id/view/:project/:*path" "update_user_handler" "update_user")
                                    ])]))
(println http-rtbl)
(println (find-route-in-http-route-table http-rtbl "update_user"))

(println (http-route-table-match http-rtbl :GET test-url))
(println (reverse-in-http-route-table http-rtbl "update_user" "433" "test-project" "github.com/zoowii"))