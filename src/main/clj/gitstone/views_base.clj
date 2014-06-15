(ns gitstone.views-base)

;;; 一些基础的函数,比如权限判断,封装response,一定条件下重定向等
;;; 封装成类似(def handler (-> handler with-access-repo with-login with-repo-exist))这类用法


