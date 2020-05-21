## 最终一致性中间件-版本变更说明

### 版本：0.0.2 日期：2019/01/15
1. 调整注解@SerialNumber的位置，可在实体属性上添加该注解；减少接口参数；
2. 调整重放锁，修复在目标接口实例或方法找不到异常时，无法释放锁的问题；
3. 放开在最终一致性方法上添加事务注解的限制，控制只允许添加Propagation.REQUIRED传播事务且非只读事务；


### 版本：0.0.3 日期：2019/01/28
1. 使用kryo序列化方式，对方法调用时的参数快照进行序列化存储，映射到数据库中的类型是longblob。解决使用fastjson序列化后反序列化泛型丢失的问题；（注：使用kryo序列化，避免项目中使用devtools开启热部署，否则会出现反序列化类型转换异常）。        
2. 修改@EventuallyConsistent注解中的concurrent属性默认值为true，因为大多数场景下接口是没有顺序的；    
3. 在事务提交后触发异步调用事件重放，这样可以减小数据一致性的延迟，并可提交性能。定时任务只作为一个补偿手段，大部分调用应该在本地事务完成后就立即异步调用了。


### 版本：0.0.4 日期： 2019/06/26
1. 去掉插入sql中id属性。默认情况下，mysql能自动处理id字段，该字段是自增的。但使用sharding-jdbc时发现，指定id字段并在插入赋值时给定null,会被sharding-jdbc解析成'null'，导致入库失败

### 版本：0.0.5 日期：2019/07/11
1. ConsistentEventMapper中使用注解方式的sql，在设置空值时报以下异常，需要指定具体的jdbcType。    

```
Caused by: org.apache.ibatis.type.TypeException: Could not set parameters for mapping: ParameterMapping{property='apiArgTypes', mode=IN, javaType=class java.lang.String, jdbcType=null, numericScale=null, resultMapId='null', jdbcTypeName='null', expression='null'}. Cause: org.apache.ibatis.type.TypeException: JDBC requires that the JdbcType must be specified for all nullable parameters.
```

在设置属性值时，指定jdbcType,如下：

```
#{appName, jdbcType=VARCHAR}, 
```

### 版本：0.0.6 日期: 2019/08/01
1. com.xiangshang.consistent.domain.business.impl.ConsistentEventBusinessImpl.replayEvent方法中catch块的代码抽取出私有方法，优化代码结构；

2. 解决retry最大限制问题。
   1. 问题：在最终一致性重放执行较慢且数据量大的时候，重试的时间间隔又很短，某一个事件日志会在多次执行定时重放时被加载出来，然后被放到单独的线程中执行。这样的问题就是多个线程中都有同一个事件日志在执行，最终导致retry的限制不起作用，超过了10次。
   2. 解决方法：将并行重放创建线程之前，先判断该事件id是否被锁定，如果被锁定，删除这些正在执行的事务ID，当次不再执行。

3. 调整重放定时每次查询数据大小，和线程池队列大小。估算方法如下：假出一个线程1s处理10个重放日志，默认最大线程数为10，默认重试定时间隔为30s，那么在一次定时执行过程中，估算可重试的事件为10*10*30=3000，假如有两个实例分两片执行，那么就是6000笔。
这样可以避免日志重放查询出来很多，但线程队列放不下而被丢弃。按照这种估算方法来调整线上最终一致性线程队列和重放查询参数，使其处于较合理的状态。

4. kryo序列化时扩展了缓冲区大小为64M，防止某些大对象序列化时丢失数据，导致反序列化失败。当然，在开发过程中不建议传大对象，应进行分解。    

5. 如果是串行执行的方法，那么不注册在事务提交后立即执行的异步处理。因为此时是异步线程执行的，不能保证顺序。跳过串行方法后，由串行定时任务去串行执行，能够保证顺序。

6. 修改保存方法调用日志的事务传播方式为Propagation.MANDATORY，要要调用最终一致性方法的上层方法必须指定事务标签，否则无法插入最终一致性方法调用日志。这在测试环境会很快发现问题。
```
	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = java.lang.RuntimeException.class)
	public ConsistentEvent save(ConsistentEvent consistentEvent) {
		//... save log
	}
```

7. 去掉最终一致性开关。在之前的版本中，如果开关被设置为false，那么最终一致性方法会立即被调用，如果该方法是一个调用远程接口的方法，那么可能导致本地事务跟远程事务不一致。所以强制使用最终一致性。


### 版本：0.0.7 日期：2019/08/15
1. 添加查询最终一致性事件日志详情接口，用于展开调用参数，方便排查问题。使用方法，http://micro-service-url/consistent/endpoint/queryConsistents?eventIds=1&eventIds=2

2. 为兼容sharding-jdbc，使用其分布式主键，需要将insert中空值(为null)字段赋上默认值，否则在sharding-jdbc插入时为报异常。

3. 优化kryo序列化，调整buffer最大值，以前设置的64M过大，会影响序列化性能，调整为最大允许4M；第二个优化，使用池化kryo实例，减少频繁创建kryo导致性能下降。


### 版本：0.0.8 日期：2019/08/28
1. 修复Kryo序列化大对象时丢失序列化信息的问题。 之前的版本代码如下：      
```
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
Output output = new Output(4096, 4194304);
output.setOutputStream(outputStream);
kryo = kryoPool.borrow();
kryo.writeObject(output, obj);
bytes = output.toBytes();
output.flush();
outputStream.close();
```
这种情况下，指定了最小和最大缓冲区大小，如果对象序列化超过4096 bytes，就会往指定的outputStream中写入，而output的缓冲区总是4096，最后通过output.toBytes()获取的实际上是最后一次序列化后的缓冲区中的数据，而不是全部的数据。
最大缓冲区实际不起作用。


正确的使用方式有两种：     
1). kryo序列化时需指定Output，序列化后的byte数据数据会存入到Output缓冲区中，默认的缓冲区大小是4096 bytes，也就是4kb。如果序列化的是大对象，就需要设置一个最大缓冲区大小；如果不使用外部的Output输出流的话，可使用toBytes()方法获取序列化结果。如下示例：   
```
Output output = new Output(4096, 1024*1024*4);
kryo = kryoPool.borrow();
kryo.writeObject(output, obj);
output.close();
byte[] bytes = output.toBytes();

```

2). 如果使用外部的一个输出流作为序列化结果的输出，那么不用设置最大缓冲区，Output每次填充满缓冲区后，都会将已经序列化的byte数据刷新到指定的外部输出流，直到序列化完成为止。序列化完成后，必须先调用close()方法，再从指定的输出流中获取序列化结果。如下示例：        
```
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
// 指定一个外部的输出流，将序列化结果保存在外部输出流中
Output output = new Output(outputStream);
kryo = kryoPool.borrow();
kryo.writeObject(output, obj);
output.close();
// 从指定的输出流中获取序列化结果，而不是从output对象中获取。
bytes = outputStream.toByteArray();
```


### 版本：0.0.9 日期：2019/08/30
1. 为了区分多中心数据，需要添加字段，不同中心的最终一致性数据只能由该中心自己处理，可以避免数据不一致。
