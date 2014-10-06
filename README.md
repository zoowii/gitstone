GitStone
====
Another Github clone implemented by Java and Clojure


## Features

* 实现了Git的HTTP协议传输(使用Basic-Auth认证)
* 实现了简单的Web界面来查看和管理git repos
* 界面参考了GitBucket


** 说明

* 默认账号root/root,默认所有数据和git repos都放到$HOME/gitstone目录下

## DOING

* 用AngularJS把前端重构
* 重构MVC
* 增加interceptors和middlewares功能
* 迁移到Leiningen2

## TODO

* 提供ssh协议访问(自己实现或者修改ssh server, 可以参考twisted的实现)
* 很多异常处理
* fix some ugly code, like path join, etc.
* 把更多代码迁移到clojure,使得java和clojure融合更融洽
* session message
* 增加git push-hook等Hooks脚本
* git repo打包有BUG
* db transaction
* git diff
* git log
* Pull Requests
* log activites
* Wiki
* view commits and code and diff in commits
* 对其他数据库比如MySQL, PostgreSQL, H2等的支持
* 各种分页和表单验证,错误和成功信息提示功能
* milestone功能, 还有issues功能的完善(分配给各个协作者,标记label等)
