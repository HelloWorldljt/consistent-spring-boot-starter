package com.xiangshang.consistent.extension.thread;

import com.google.common.base.Stopwatch;
import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.domain.em.InvokeStatus;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * 事件重放线程，串行线程。一个线程顺序执行串行事件记录。保证调用顺序
 *
 * @author chenrg
 * @date 2018年12月6日
 */
public class ConsistentEventReplaySerialThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistentEventReplaySerialThread.class);

    private ConsistentEventBusiness consistentEventBusiness;

    private List<ConsistentEvent> events;

    public ConsistentEventReplaySerialThread(ConsistentEventBusiness consistentEventBusiness, List<ConsistentEvent> events) {
        super();
        this.consistentEventBusiness = consistentEventBusiness;
        this.events = events;
    }

    @Override
    public void run() {
        for (int i = 0; i < events.size(); i++) {
            ConsistentEvent event = consistentEventBusiness.get(events.get(i).getId());
            if (Objects.equals(event.getInvokeStatus(), InvokeStatus.SUCCESS)) {
                LOGGER.debug("Consistent event already success. id is [{}]", event.getId());
                continue;
            }
            LOGGER.info("Begin serial replay event, eventId:[{}], progress:[{}/{}]", event.getId(), i + 1, events.size());
            Stopwatch sw = Stopwatch.createStarted();
            consistentEventBusiness.replayEvent(event);
            LOGGER.info("Finish serial replay event, eventId:[{}], progress:[{}/{}], cost:[{}]", event.getId(), i + 1, events.size(), sw.stop().toString());
        }
    }

}
