package com.xiangshang.consistent.config.sub;

/**
 * 最终一致性事件重放线程池配置。参考{@code ThreadPoolTaskExecutor}核心配置。
 * 
 * @author chenrg
 * @date 2018年12月11日
 */
public class ThreadPoolConfig {

	/**
	 * 核心线程线程数，默认3
	 */
	private int corePoolSize = 3;

	/**
	 * 最大线程数，默认10
	 */
	private int maxPoolSize = 10;

	/**
	 * 线程队列容量，默认5000
	 */
	private int queueCapacity = 5000;

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

}
