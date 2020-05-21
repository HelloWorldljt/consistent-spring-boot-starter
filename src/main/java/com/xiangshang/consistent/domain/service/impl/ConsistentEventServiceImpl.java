package com.xiangshang.consistent.domain.service.impl;

import com.google.common.base.Joiner;
import com.xiangshang.consistent.domain.dao.ConsistentEventMapper;
import com.xiangshang.consistent.domain.em.InvokeStatus;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import com.xiangshang.consistent.domain.service.ConsistentEventService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 一致性事件服务实现
 *
 * @author chenrg
 * @date 2018年12月10日
 */
@Service
public class ConsistentEventServiceImpl implements ConsistentEventService {

    @Autowired
    private ConsistentEventMapper consistentEventMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = java.lang.RuntimeException.class)
    public ConsistentEvent save(ConsistentEvent consistentEvent) {
        consistentEvent.setInvokeStatus(InvokeStatus.INIT);
        consistentEvent.setCreateTime(new Date());
        consistentEvent.setUpdateTime(new Date());
        consistentEvent.setApiRetValue(StringUtils.isNotBlank(consistentEvent.getApiRetValue()) ? consistentEvent.getApiRetValue() : "");
        consistentEvent.setApiArgTypes(StringUtils.isNotBlank(consistentEvent.getApiArgTypes()) ? consistentEvent.getApiArgTypes() : "");
        consistentEvent.setApiArgValues(StringUtils.isNotBlank(consistentEvent.getApiArgValues()) ? consistentEvent.getApiArgValues() : "");
        consistentEvent.setSerialNumber(StringUtils.isNotBlank(consistentEvent.getSerialNumber()) ? consistentEvent.getSerialNumber() : "");
        consistentEvent.setRemark(StringUtils.isNotBlank(consistentEvent.getRemark()) ? consistentEvent.getRemark() : "");
        consistentEvent.setRetry(0);
        consistentEvent.setVersion(0);
        consistentEventMapper.insert(consistentEvent);
        return consistentEvent;
    }

    @Override
    public ConsistentEvent get(Long id) {
        return consistentEventMapper.get(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = java.lang.RuntimeException.class)
    public void updateSuccess(Long id, String apiRetValue, Integer version) {
        consistentEventMapper.updateStatus(id, apiRetValue, version, InvokeStatus.SUCCESS.name(), new Date(), "OK");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = java.lang.RuntimeException.class)
    public void updateFailed(Long id, Integer version, String remark) {
        consistentEventMapper.updateStatus(id, null, version, InvokeStatus.FAILED.name(), new Date(), remark);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = java.lang.RuntimeException.class)
    public void updateUnknown(Long id, Integer version, String remark) {
        consistentEventMapper.updateStatus(id, null, version, InvokeStatus.UNKNOWN.name(), new Date(), remark);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = java.lang.RuntimeException.class)
    public void updateProcessing(Long id, Integer version, String remark) {
        consistentEventMapper.updateStatus(id, null, version, InvokeStatus.PROCESSING.name(), new Date(), remark);
    }

    @Override
    public List<Long> getNeedReplayEventIds(String appName, String centerId, int retryLimit, int pageSize, boolean concurrentReplay) {
        return consistentEventMapper.getNeedReplayEventIds(appName, centerId, retryLimit, InvokeStatus.INIT.name(),
                String.format("'%1s','%2s'", InvokeStatus.PROCESSING.name(), InvokeStatus.UNKNOWN.name()), new Date(), pageSize, concurrentReplay);
    }

    @Override
    public List<ConsistentEvent> getNeedReplayEventWithAttributes(String appName, String centerId, int retryLimit, int pageSize, boolean concurrentReplay) {
        return consistentEventMapper.getNeedReplayEventWithAttributes(appName, centerId, retryLimit, InvokeStatus.INIT.name(),
                String.format("'%1s','%2s'", InvokeStatus.PROCESSING.name(), InvokeStatus.UNKNOWN.name()), new Date(), pageSize, concurrentReplay);
    }

    @Override
    public List<Long> getNeedCleanEvents(String appName, String centerId, int retainDays, int pageSize) {
        Date date = DateUtils.addDays(new Date(), -retainDays);
        return consistentEventMapper.getNeedCleanEvents(appName, centerId, InvokeStatus.SUCCESS, date, pageSize);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = java.lang.RuntimeException.class)
    public void cleanEvents(List<Long> pageIds) {
        consistentEventMapper.deleteByIds(Joiner.on(",").join(pageIds));
    }

    @Override
    public List<ConsistentEvent> getConsistentEvents(String appName, List<Long> eventIds) {
        return consistentEventMapper.queryConsistentEvents(appName, eventIds);
    }
}
