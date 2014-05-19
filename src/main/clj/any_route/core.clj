(ns any-route.core)

;; 定义一组规则,将字符串(比如url)双射到一个名称
;; (比如常见Web MVC框架中的controller-action, params为source str中捕获的值)
;; 要同时支持双向映射
;; 规则允许嵌套,比如(context "/api" [["/user" "user"] ["/role" "role"]])
;; 为了捕获params, 需要有分隔符的定义,默认分隔符是'/',也可以改为其他
;; source中捕获形如:abc, :*abc 的内容, 后者可以跨分隔符捕获参数,前者不可以

;; 实现可以参考compojure的路由实现(但是compojure没有实现双射)

;; request是形如{:source "/abc/def"}的对象

;; 这个库Doing中,应该会有很多地方抄clout

(defn prepare-route [route]
  "pre-compile the route rule")

(defn- assoc-route-params
  "Associate route parameters with the request map."
  [request params]
  (merge-with merge request {:route-params params, :params params}))

(defn if-route
  "返回一个函数,这个函数只有在route匹配时才会返回匹配结果"
  [route handler]
  "dummy")

(defn make-route
  "创建一个只有在route规则匹配时才会返回非false/nil的函数(实际返回的应该是dest和params)"
  [route dest]
  "dummy")

(defn compile-route [route]
  "定义一条source => dest的规则")

(defn route-match
  "将路由和实际值进行匹配"
  [route request]
  "dummy")

(defn context-route [ctx rules]
  "嵌套路由")

