## 最终一致性中间件


### 1、背景
1. 向上微服务拆分和微服务体系建立，系统调用复杂度增高，分布式事务作为重要的一环被纳入微服务管理中。
2. 两阶段提交或TCC分布式事务对业务耦合度高，锁定资源和回滚较难控制，实现成本高，一般都不建议采用强一致性事务。
3. 向上目前的项目中多采用最终一致性方案，实现方式有消息通知和事件重试，有很多重复性的表和功能代码，不够优雅。
4. 在此背景下，需要对最终一致性实现进行抽象，减少与业务的耦合。

### 2、实现思路
1. 标记调用三方接口的本地方法为支持最终一致性方法；
2. 将该方法调用置于本地事务内；
3. 使用AOP拦截该方法的执行，在方法被调用之前插入一条调用事件记录，并与本地事务一起提交，然后直接返回，该方法不被立即调用；调用事件记录将方法签名和参数列表进行保存；
4. 开启一个定时任务，扫描未调用或调用状态未知的所有事件记录，通过反射循环进行接口重放，并根据调用接口的成功与否，更新事件记录。
5. 重试机制，如果在接口重放没有明确的返回或异常，比如超时，则下次进行重试，重试次数可配置。

### 3、程序设计
1. 注解EventuallyConsistent用于标记方法是支持最终一致性方法。	
	- 支持串行、并行重放；
	- 支持指定重试时间间隔，单位分钟； 
	- 支持指定异常重试；
2. 注解SerialNumber标记方法某个参数为流水号，用于记录流水号。
3. EventuallyConsistentAspect 拦截注解的方法，根据本地线程中是否持有事件记录ID来进行初始化调用事件或重放接口。
4. ConsistentEventConcurrentReplayJob, ConsistentEventSerialReplayJob  接口重放定时任务，对初始状态或未知状态、处理中状态的记录进行接口重放。支持并生重放和串行重放
5. ConsistentEventCleanJob 清理记录任务，将x天之前的成功记录进行删除，减小数据量，提高执行效率，数据保留天数可配置。
6. 使用mybatis作为orm层。
7. 使用elastic-job集成定时任务。
8. 基于spring-boot开发，不支持老的项目。
9. 使用jdk1.8及以上版本

### 4、集成方法
1、在目标项目的pom.xml中添加依赖：

```
<dependency>
    <groupId>com.xiangshang.consistent</groupId>
    <artifactId>consistent-spring-boot-starter</artifactId>
    <version>0.0.1</version>
    <exclusions>
        <exclusion>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

2、添加consistent-spring-boot-starter的配置项，如果确认默认值不需要更改的话，可省略所有配置。配置项如下：

```
# 配置最终一致性
eventually-consistent:
  # 是否开启中间件
  enable: true
  # 重试次数
  retry-limit: 3
  # 保留数据天数，默认7天
  retain-days: 7
  # 清理时分页大小
  clean-page-size: 1000
```

4、添加elastic-job配置项，用于支持定时任务。如下示例：

```
# 配置elastic-job    
elasticjob:
  enable: true
  job-collect: false
  zookeeper:
    server-lists: 10.200.1.222:2181 
    namespace: consistent
  notice-mobile-pattern: '[1][3578]\d{9}'
```

5、在需要支持最终一致性的方法上添加注解EventuallyConsistent即可。

6、现在，启动项目，检查是否运行正常。

### 5、适用场景
1. 本地业务完成后，被通知方的业务必须成功；
2. 允许短时间的数据不一致；
3. 最终一致性的方法必须在本地事务方法中调用，这样保证本地事务提交后，最终一致性的方法调用日志也一起被记录。

### 6、注意事项
1、 最终一致性接口需要支持幂等。
			
2、 中间件在mapper中使用的注解方式添加sql，mybatis需要自动识别，所以须在mybatis-config.xml中配置上以下属性：

```
<!-- 自动驼峰命名转换 -->
<setting name="mapUnderscoreToCamelCase" value="true" />
```

3、 在被EventuallyConsistent标记的方法上不要添加事务或异步等其它注解。保证该方法必须包含在本地事务之内。

4、中间件中的三个定时任务都有默认的执行时间，事件重放的定时任务是默认1分种执行一次，如果需要进行调整，可以在配置中进行指定。

```
# 配置最终一致性中间件
eventually-consistent:
  # 最终一致性中间件定时任务配置
  jobs:
    # 并行事件重放定时配置
    concurrent-replay:  
      cron: '0/30 * * * * ?' # 
      sharding-total-count: 2
    # 串行事件重放定时配置
    serial-replay:
      cron: '0/30 * * * * ?'
    # 清理事件记录定时配置
    clean: 
      cron: '0 30 12 * * ?'
```


5、 关于串行重放顺序如何保证。  
在某些场景下（比如出入账操作），要求必须保证接口调用的前后顺序，通过EventuallyConsistent注解中的concurrent属性进行标记，串行接口请标记为false，默认是串行，0.0.3版本后改为默认并行。  
串行事件重放单独一个定时任务，根据接口签名进行分组，每个线程处理一个接口的串行重放。  
通过在EventuallyConsistent注解中的interruptFor属性来指定在哪些异常情况下需要中断串行重放。为保证绝对的顺序，则必须在串行模式下指定该属性值。重放时如果捕获到interruptFor指定的异常，就抛出异常SerialReplayInterruptedException，中断该线程的执行。  


6、使用注解的方法必须是在单独一个类中的方法。这是由Spring AOP 动态代理决定的，在同一个类中调用另一个被注解标记的方法，注解无法进行拦截。事务@Transactional注解也有同样的问题。



### 7、持续改进 
1. 屏蔽掉dao层操作，与orm框架无关的方式，待调研。