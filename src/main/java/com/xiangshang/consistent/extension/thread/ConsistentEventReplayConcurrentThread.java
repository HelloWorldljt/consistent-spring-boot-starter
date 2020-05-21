package com.xiangshang.consistent.extension.thread;

import com.google.common.base.Stopwatch;
import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.domain.em.InvokeStatus;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 事件重放线程，并行线程。一个线程处理一条记录
 *
 * @author chenrg
 * @date 2018年12月6日
 */
public class ConsistentEventReplayConcurrentThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistentEventReplayConcurrentThread.class);

    private ConsistentEventBusiness consistentEventBusiness;

    /**
     * 待重放的事件记录ID
     */
    private Long eventId;

    /**
     * 当前处理的事件记录索引位置
     */
    private int current;

    /**
     * 批量处理的总数量
     */
    private int total;

    public ConsistentEventReplayConcurrentThread(ConsistentEventBusiness consistentEventBusiness, Long eventId, int current,
                                                 int total) {
        super();
        this.consistentEventBusiness = consistentEventBusiness;
        this.eventId = eventId;
        this.current = current;
        this.total = total;
    }

    @Override
    public void run() {
        ConsistentEvent event = consistentEventBusiness.get(eventId);
        if (Objects.equals(event.getInvokeStatus(), InvokeStatus.SUCCESS)) {
            LOGGER.debug("Consistent event already success. id is [{}]", event.getId());
            return;
        }
        try {
            Stopwatch sw = Stopwatch.createStarted();
            consistentEventBusiness.replayEvent(event);
            LOGGER.info("Finish concurrent replay event, eventId:[{}], progress:[{}/{}], cost:[{}]", event.getId(), current + 1, total, sw.stop().toString());
        } catch (Exception e) {
            LOGGER.error("Concurrent replay event occured exception. eventId:[{}]", e);
        }
    }

}
