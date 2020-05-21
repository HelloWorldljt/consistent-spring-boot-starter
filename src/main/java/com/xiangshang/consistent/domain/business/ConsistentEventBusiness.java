package com.xiangshang.consistent.domain.business;

import com.xiangshang.consistent.domain.model.ConsistentEvent;

import java.util.List;

/**
 * 事件处理业务接口
 *
 * @author chenrg
 * @date 2018年12月10日
 */
public interface ConsistentEventBusiness {

    /**
     * 保存一致性事件记录
     *
     * @param consistentEvent
     */
    void saveConsistentEvent(ConsistentEvent consistentEvent);

    /**
     * 根据主键获取单条事件记录
     *
     * @param id
     * @return
     */
    ConsistentEvent get(Long id);

    /**
     * 获取需要并行接口重放的事件记录
     *
     * @return
     */
    List<Long> getNeedConcurrentReplayEvents();

    /**
     * 获取需要串行接口重放的事件记录
     *
     * @return
     */
    List<ConsistentEvent> getNeedSerialReplayEvents();

    /**
     * 重放接口
     *
     * @param event
     */
    void replayEvent(ConsistentEvent event);

    /**
     * 清理成功的事件记录数据。根据保留天数，retainDays天之前的成功记录会被清理
     */
    void cleanEvents();

    /**
     * 根据事件ID列表查询事件详情
     *
     * @param eventIds
     * @return
     */
    List<ConsistentEvent> getConsistentEvents(List<Long> eventIds);
}