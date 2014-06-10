GitStone
====
Another Github clone implemented by Java and Clojure


## Features

* 实现了Git的HTTP协议传输(使用Basic-Auth认证)
* 实现了简单的Web界面来查看和管理git repos
* 界面参考了GitBucket


** 说明

* 默认账号root/root,默认所有数据和git repos都放到$HOME/gitstone目录下


## TODO

* 提供ssh协议访问(自己实现或者修改ssh server,或者看有没有办法拦截ssh请求)
* 路由增加before/after actions
* 很多异常处理
* fix some ugly code, like path join, etc.
* 把更多代码迁移到clojure,使得java和clojure融合更融洽
* 换个ORM或者db abstract dao框架
* session message
* 增加git push-hook等Hooks脚本
* git repo打包有BUG
* db transaction
* git diff
* git log
* Issues
* Pull Requests
* log activites
* Wiki