package com.xiangshang.consistent.domain.task;

import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.extension.context.ConsistentEventReplayLock;
import com.xiangshang.consistent.extension.thread.ConsistentEventReplayConcurrentThread;
import com.xiangshang.elasticjob.lite.starter.annotation.ElasticSimpleJob;
import com.xiangshang.elasticjob.lite.starter.job.AbstractMultipleSliceJob;
import io.elasticjob.lite.api.ShardingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件重放任务
 *
 * @author chenrg
 * @date 2018年12月4日
 */
@Component
@ElasticSimpleJob(appName = "${spring.application.name}",
        jobClass = ConsistentEventConcurrentReplayJob.class,
        cron = "${eventually-consistent.jobs.concurrent-replay.cron:0 0/1 * * * ?}",
        shardingTotalCount = "${eventually-consistent.jobs.concurrent-replay.sharding-total-count:3}",
        description = "最终一致性标签方法调用事件重放任务-并行",
        needWarn = false, persistJobStatus = false)
public class ConsistentEventConcurrentReplayJob extends AbstractMultipleSliceJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistentEventConcurrentReplayJob.class);

    @Autowired
    private ConsistentEventBusiness consistentEventBusiness;

    @Resource(name = "eventuallyConsistentExecutor")
    private ThreadPoolTaskExecutor executor;

    @Override
    public void doTask(ShardingContext content) {
        LOGGER.info("Replay consistent event concurrently by multiple slice job. current sharding item:[{}/{}].", content.getShardingItem(),
                content.getShardingTotalCount());

        List<Long> eventIds = consistentEventBusiness.getNeedConcurrentReplayEvents();

        if (CollectionUtils.isEmpty(eventIds)) {
            LOGGER.info("There was no consistent event need to concurrent replay.");
            return;
        }

        // 按id取模进行分片
        eventIds = eventIds.stream().filter(eventId -> eventId % content.getShardingTotalCount() == content.getShardingItem()).collect(Collectors.toList());

        // 过滤掉正在执行的事件ID
        ConsistentEventReplayLock.removeLockedEventIds(eventIds);

        for (int i = 0; i < eventIds.size(); i++) {
            Long eventId = eventIds.get(i);
            executor.execute(new ConsistentEventReplayConcurrentThread(consistentEventBusiness, eventId, i, eventIds.size()));
        }
    }

}
