## Tomcat Server 实现

### 支持的配置参数 option

* protocol - Tomcat Connector 对应的 handler，默认为：org.apache.coyote.http11.Http11NioProtocol
* uri_encoding=UTF-8 - 处理URI时的路径编码
* reloadable=true - 是否载入
* cache_size=16384 - Tomcat启动时如果资源过多会引起cache警告，通过此参数控制cache容量大小，其中
  * 值必须为整数
  * -1 - 关闭cache
  * 0 - 为恢复Tomcat默认模式
* default_web_xml - 默认的web.xml路径（TODO 目前未生效 待解决）
* classes_dir - 在直接运行模式中所定义的 classes_dir，一般指向构建工程中被包含到WEB-INF/classes的build out目录
* tomcat_user - Tomcat 用户配置。为了加入多个用户，此配置会查找所有以 tomcat_user 开头的变量作为用户声明
  * 多个用户可以使用 tomcat_user1 tomcat_user2 tomcat_user3 等多个参数名来配置，每个参数名表示对应的一个用户
  * 每个用户配置格式为： `user:pass:roles` roles多个值用 `,` 分隔
* 待补充...
