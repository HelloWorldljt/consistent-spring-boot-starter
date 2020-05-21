package com.xiangshang.consistent.domain.service;

import java.util.List;

import com.xiangshang.consistent.domain.model.ConsistentEvent;

/**
 * 一致性事件服务接口
 * 
 * @author chenrg
 * @date 2018年12月10日
 */
public interface ConsistentEventService {

	/**
	 * 插入
	 * 
	 * @param consistentEvent
	 * @return
	 */
	ConsistentEvent save(ConsistentEvent consistentEvent);

	/**
	 * 根据ID获取记录
	 * 
	 * @param id
	 * @return
	 */
	ConsistentEvent get(Long id);

	/**
	 * 更新为成功
	 * 
	 * @param id
	 * @param apiRetValue
	 * @param version
	 */
	void updateSuccess(Long id, String apiRetValue, Integer version);

	/**
	 * 更新为失败
	 * 
	 * @param id
	 * @param version
	 * @param remark
	 */
	void updateFailed(Long id, Integer version, String remark);

	/**
	 * 更新为未知状态
	 * 
	 * @param id
	 * @param version
	 * @param remark
	 */
	void updateUnknown(Long id, Integer version, String remark);

	/**
	 * 更新为处理中状态
	 * 
	 * @param id
	 * @param version
	 * @param remark
	 */
	void updateProcessing(Long id, Integer version, String remark);

	/**
	 * 查询本应用下未调用事件记录，或者调用状态未知且retry次数未达上限的事件记录
	 *
	 * @param appName
	 * @param centerId
	 * @param retryLimit
	 * @param pageSize
	 * @param concurrentReplay
	 *            是否并行重放
	 * @return
	 */
	List<Long> getNeedReplayEventIds(String appName, String centerId, int retryLimit, int pageSize, boolean concurrentReplay);

	/**
	 * 查询本应用下未调用事件记录，或者调用状态未知且retry次数未达上限的事件记录，反回事件所有属性
	 * @param appName
	 * @param centerId
     * @param retryLimit
     * @param pageSize
     * @param concurrentReplay
     * @return
	 */
	List<ConsistentEvent> getNeedReplayEventWithAttributes(String appName, String centerId, int retryLimit, int pageSize, boolean concurrentReplay);

	/**
	 * 查询待清理事件记录ID。条件：retainDays日之前的成功记录。
	 * <p>
	 * 只清理当前app的数据
	 * <p>
	 *
	 * @param appName
	 * @param centerId
     * @param retainDays
     * @param pageSize
     * @return
	 */
	List<Long> getNeedCleanEvents(String appName, String centerId, int retainDays, int pageSize);

	/**
	 * 清理指定id的记录
	 *
	 * @param pageIds
	 */
	void cleanEvents(List<Long> pageIds);

	/**
	 * 查询事件详情
	 * @param appName
	 * @param eventIds
	 * @return
	 */
    List<ConsistentEvent> getConsistentEvents(String appName, List<Long> eventIds);
}
