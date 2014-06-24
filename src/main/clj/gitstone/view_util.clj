(ns gitstone.view-util
  (:import (com.zoowii.mvc.http HttpRequest HttpResponse))
  (:require [clojure.tools.logging :as logging]
            [gitstone.web :as web]
            [gitstone.util :as util]
            [gitstone.db :as db]
            [gitstone.repo-dao :as repo-dao]))

(defn asset-can-access-repo-wrapper
  "确保请求的repo存在且当前用户有权限访问"                                   ;; TODO: 根据是否是AJAX请求来判断返回格式
  ([handler]
   (asset-can-access-repo-wrapper handler #(web/url-for "index")))
  ([handler redirect-url-fn]
   (fn [^HttpRequest req ^HttpResponse res]
     (let [cur-user (web/current-user req)
           repo-name (.getParam req "repo")
           repo (db/find-repo-by-name-and-user repo-name cur-user)]
       (if (not repo)
         (web/redirect res (redirect-url-fn))
         (if (not (repo-dao/can-access-repo repo cur-user))
           (web/redirect res (redirect-url-fn))
           (handler req res)))))))

(defn response-wrapper
  ([handler]
   (fn [req res]
     (web/response
       res
       (handler req res)))))
