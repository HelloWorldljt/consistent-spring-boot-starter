package com.xiangshang.consistent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * <p>
 * 最终一致性标签。被标记的方法是一个调用外部系统的接口或实现方法。
 * </p>
 * <p>
 * step1.实现最终一致性的思想是：被标记的方法调用应处于外层方法的本地事务中，通过拦截该方法执行前，在本地事务中插入一条最终一致性调用事件记录，并当即返回。<br>
 * step2.当前本地事务提交成功后异步方式发起最终一致性方法的调用；或者通过定时任务扫描未处理的调用事件记录，根据记录的接口方法反射获取接口实例进行调用。
 * </p>
 * <p>
 * 要点：</br>
 * 1).每个集成的项目所连的数据库应创建一个consistent_event表。</br>
 * </p>
 * 
 * @author chenrg
 * @date 2018年12月4日
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface EventuallyConsistent {

	/**
	 * 接口是否支持并行重放，默认为false。如果确认接口调用无顺序，可以指定并行处理
	 * 
	 * @return
	 */
	public boolean concurrent() default true;

	/**
	 * 重试时间间隔，单位分钟。默认5分钟。但最小重试间隔时间取决于接口重放定时任务{@code ConsistentEventReplayJob}的最短间隔时间。
	 * 
	 * @return
	 */
	public int retryInterval() default 5;

	/**
	 * 指定哪些异常需要重试。在非http异常的情况下，根据接口返回的数据，判断当前业务是否需要重试，如果需要重试，抛出retryFor指定的异常，中间件会根据抛出的指定异常进行重试。
	 * 
	 * @return
	 */
	Class<? extends Throwable>[] retryFor() default {};

	/**
	 * 在concurrent=false的情况下，即以串行方式执行时，为保证执行顺序，需要在异常情况下中断后续的串行重放。<p>
	 * 该属性指定在什么异常情况下需要中断串行重放。<p>
	 *
	 * @return
	 */
	Class<? extends Throwable>[] interruptFor() default {};

}
