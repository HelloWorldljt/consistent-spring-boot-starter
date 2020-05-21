package com.xiangshang.consistent.domain.em;

/**
 * 一致性方法调用状态。为减少依赖，不使用int值
 * 
 * @author chenrg
 * @date 2018年12月4日
 */
public enum InvokeStatus {
	/**
	 * 初始记录
	 */
	INIT,
	/**
	 * 处理中，由接口返回参数判断，未完成本次业务的。根据retryFor抛出的指定异常，更新为处理中状态。
	 */
	PROCESSING,
	/**
	 * 成功
	 */
	SUCCESS,
	/**
	 * 失败
	 */
	FAILED,
	/**
	 * 未知状态，在http调用超时，rest调用异常。这种情况并不能确认是否调用成功，需要进行重试。
	 */
	UNKNOWN;

}
