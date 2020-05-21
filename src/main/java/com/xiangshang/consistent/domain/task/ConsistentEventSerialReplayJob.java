package com.xiangshang.consistent.domain.task;

import com.xiangshang.consistent.config.EventuallyConsistentConfig;
import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import com.xiangshang.consistent.extension.context.ConsistentEventReplayLock;
import com.xiangshang.consistent.extension.thread.ConsistentEventReplaySerialThread;
import com.xiangshang.elasticjob.lite.starter.annotation.ElasticSimpleJob;
import com.xiangshang.elasticjob.lite.starter.job.AbstractSingleSliceJob;
import io.elasticjob.lite.api.ShardingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 事件重放任务。串行重放。
 * <p>
 * 单片执行，最大程度的保证顺序调用。不考虑失败的事件重放。
 * <p>
 *
 * @author chenrg
 * @date 2018年12月4日
 */
@Component
@ElasticSimpleJob(appName = "${spring.application.name}",
        jobClass = ConsistentEventSerialReplayJob.class,
        cron = "${eventually-consistent.jobs.serial-replay.cron:0 0/1 * * * ?}",
        description = "最终一致性标签方法调用事件重放任务-串行",
        needWarn = false, persistJobStatus = false)
public class ConsistentEventSerialReplayJob extends AbstractSingleSliceJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistentEventSerialReplayJob.class);

    @Autowired
    private ConsistentEventBusiness consistentEventBusiness;

    @Resource(name = "eventuallyConsistentExecutor")
    private ThreadPoolTaskExecutor executor;

    @Override
    public void doTask(ShardingContext content) {
        // 查询初始化状态、未知状态且重试次数在最大限制次数内的事件记录
        List<ConsistentEvent> events = consistentEventBusiness.getNeedSerialReplayEvents();

        if (CollectionUtils.isEmpty(events)) {
            LOGGER.info("There was no consistent event need to serial replay.");
            return;
        }

        // 过滤掉正在执行的事件
        List<Long> eventIds = events.stream().map(event -> event.getId()).collect(Collectors.toList());
        ConsistentEventReplayLock.removeLockedEventIds(eventIds);
        events = events.stream().filter(event -> eventIds.contains(event.getId())).collect(Collectors.toList());

        // 按方法签名进行分组，每个串行接口一个线程处理
        Map<String, List<ConsistentEvent>> maps = events.stream().collect(Collectors.groupingBy(ConsistentEvent::getApiSignature));
        Collection<List<ConsistentEvent>> values = maps.values();
        for (List<ConsistentEvent> groupByApiSignature : values) {
            executor.execute(new ConsistentEventReplaySerialThread(consistentEventBusiness, groupByApiSignature));
        }
    }

}
