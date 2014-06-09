GitStone
====
Another github clone implemented by Java and Clojure


## Features

* 实现了Git的HTTP协议传输(使用Basic-Auth认证)


## TODO

* 提供ssh协议访问(自己实现或者修改ssh server,或者看有没有办法拦截ssh请求)
* 路由不仅要可以路由到java类,还要可以路由到clojure函数
* 路由增加before/after actions
* 很多异常处理
* fix some ugly code, like path join, etc.
* 把更多代码迁移到clojure,使得java和clojure融合更融洽
* 换个ORM或者db abstract dao框架
* session message
* 增加git push-hook等Hooks脚本
* git repo打包有BUG