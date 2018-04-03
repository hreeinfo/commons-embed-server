## Jetty Server 实现

### 支持的配置参数 option

* jetty_classpaths=true - classpathdirs使用jetty内部的classpath机制，而不是使用启动线程的contextloader
* jetty_configurations=*,other_class_name - 用于重写jetty的configurations
  * 每个configuration为完整类名，多个值之间用,分隔
  * 如果以包含*，仅增加额外的配置，出现*的位置用默认实例填充；否则以当前定义值重写整体configurations
* 待补充...