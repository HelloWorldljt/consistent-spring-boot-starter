package com.xiangshang.consistent.domain.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.elasticjob.lite.starter.annotation.ElasticSimpleJob;
import com.xiangshang.elasticjob.lite.starter.job.AbstractSingleSliceJob;

import io.elasticjob.lite.api.ShardingContext;

/**
 * 清理已经成功的事件记录。保持事件记录表数据最小，以便提高sql执行效率。
 * 
 * @author chenrg
 * @date 2018年12月5日
 */
@Component
@ElasticSimpleJob(appName = "${spring.application.name}", 
	jobClass = ConsistentEventCleanJob.class, 
	cron = "${eventually-consistent.jobs.clean.cron:0 0 22 * * ?}", 
	description = "最终一致性标签方法调用事件清理任务", 
	needWarn = false, persistJobStatus = false)
public class ConsistentEventCleanJob extends AbstractSingleSliceJob {

	@Autowired
	private ConsistentEventBusiness consistentEventBusiness;

	@Override
	public void doTask(ShardingContext content) {
		consistentEventBusiness.cleanEvents();
	}

}
