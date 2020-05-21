package com.xiangshang.consistent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.xiangshang.consistent.annotation.EventuallyConsistent;
import com.xiangshang.consistent.config.sub.ThreadPoolConfig;

/**
 * 最终一致性插件相关配置
 * 
 * @author chenrg
 * @date 2018年12月4日
 */
@ConfigurationProperties(prefix = "eventually-consistent")
public class EventuallyConsistentConfig {

	/**
	 * 最大重试次数，默认10次
	 */
	private int retryLimit = 10;

	/**
	 * 记录保留天数。保留最近几日的数据，之前的成功记录会定时清理。默认保留7天
	 */
	private int retainDays = 7;

	/**
	 * 清理记录分页大小，默认5000
	 */
	private int cleanPageSize = 5000;

	/**
	 * 一次查询待处理事件记录条数限制。默认10000条。防止一次读取太多
	 */
	public int replayPageSize = 10000;

	/**
	 * 配置执行事件重放的线程池
	 */
	private ThreadPoolConfig threadPool = new ThreadPoolConfig();

	public int getRetryLimit() {
		return retryLimit;
	}

	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	public int getCleanPageSize() {
		return cleanPageSize;
	}

	public void setCleanPageSize(int cleanPageSize) {
		this.cleanPageSize = cleanPageSize;
	}

	public int getRetainDays() {
		return retainDays;
	}

	public void setRetainDays(int retainDays) {
		this.retainDays = retainDays;
	}

	public int getReplayPageSize() {
		return replayPageSize;
	}

	public void setReplayPageSize(int replayPageSize) {
		this.replayPageSize = replayPageSize;
	}

	public ThreadPoolConfig getThreadPool() {
		return threadPool;
	}

	public void setThreadPool(ThreadPoolConfig threadPool) {
		this.threadPool = threadPool;
	}

}
