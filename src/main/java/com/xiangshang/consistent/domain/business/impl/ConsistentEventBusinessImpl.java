package com.xiangshang.consistent.domain.business.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Stopwatch;
import com.xiangshang.consistent.annotation.EventuallyConsistent;
import com.xiangshang.consistent.config.EventuallyConsistentConfig;
import com.xiangshang.consistent.config.EventuallyDataCenterConfig;
import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import com.xiangshang.consistent.domain.service.ConsistentEventService;
import com.xiangshang.consistent.extension.context.ApplicationContextHolder;
import com.xiangshang.consistent.extension.context.ConsistentEventReplayHolder;
import com.xiangshang.consistent.extension.context.ConsistentEventReplayLock;
import com.xiangshang.consistent.extension.exception.SerialReplayInterruptedException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * {@link ConsistentEvent}业务处理
 *
 * @author chenrg
 * @date 2018年12月5日
 */
@Service
public class ConsistentEventBusinessImpl implements ConsistentEventBusiness {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistentEventBusinessImpl.class);

    private static final String TIMEOUT = "timeout";

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private EventuallyConsistentConfig config;

    @Autowired
    private EventuallyDataCenterConfig dataCenterConfig;

    @Autowired
    private ConsistentEventService consistentEventService;

    @Override
    public void saveConsistentEvent(ConsistentEvent consistentEvent) {
        consistentEventService.save(consistentEvent);
    }

    @Override
    public ConsistentEvent get(Long id) {
        return consistentEventService.get(id);
    }

    @Override
    public List<Long> getNeedConcurrentReplayEvents() {
        return consistentEventService.getNeedReplayEventIds(appName, dataCenterConfig.getCenterId(), config.getRetryLimit(), config.getReplayPageSize(), true);
    }

    @Override
    public List<ConsistentEvent> getNeedSerialReplayEvents() {
        return consistentEventService.getNeedReplayEventWithAttributes(appName, dataCenterConfig.getCenterId(), config.getRetryLimit(), config.getReplayPageSize(), false);
    }

    @Override
    public void replayEvent(ConsistentEvent event) {

        Long eventId = event.getId();

        // 根据事件记录，获取事件调用接口签名和参数。
        Class<?> apiClass = null;
        try {
            apiClass = Class.forName(event.getApiClass());
        } catch (ClassNotFoundException e) {
            LOGGER.error("Can't find the class type for name:[{}]", event.getApiClass(), e);
            return;
        }
        // 通过反射获取接口实例
        Object target = ApplicationContextHolder.getBean(apiClass);

        // 做兼容，v0.0.3版本改为使用kryo进行快照序列化。之前的版本使用json。

        // 获取参数类型列表
        Class<?>[] parameterTypes = null;
        // 获取参数值
        Object[] args = null;

        if (Objects.nonNull(event.getApiInvocation())) {
            parameterTypes = event.getApiInvocation().getArgTypes();
            args = event.getApiInvocation().getArgValues();
        } else if (StringUtils.isNotBlank(event.getApiArgTypes()) && StringUtils.isNotBlank(event.getApiArgValues())) {
            parameterTypes = JSONObject.parseObject(event.getApiArgTypes(), Class[].class);
            List<Object> argList = JSONObject.parseArray(event.getApiArgValues(), parameterTypes);
            args = CollectionUtils.isEmpty(argList) ? null : argList.toArray();
        }
        // 通过反射获取接口方法
        Method method = ReflectionUtils.findMethod(apiClass, event.getApiMethod(), parameterTypes);
        if (method == null) {
            LOGGER.error("Can't find the method :{}.{}", event.getApiClass(), event.getApiMethod());
            return;
        }

        // 判断是否被锁定
        if (ConsistentEventReplayLock.isLocked(eventId)) {
            LOGGER.info("The event is already being processed. id is:[{}]", eventId);
            return;
        }

        // 处理前锁定
        ConsistentEventReplayLock.lock(eventId);

        // 将事件ID放入当前线程变量
        ConsistentEventReplayHolder.setCurrentEventId(eventId);

        // 重放接口调用
        try {
            Object result = ReflectionUtils.invokeMethod(method, target, args);
            String retValue = result != null ? JSONObject.toJSONString(result) : null;
            // 更新事件记录为处理成功
            consistentEventService.updateSuccess(event.getId(), retValue, event.getVersion());
        } catch (Throwable ex) {
            LOGGER.error("Replay interface exception. interface is [{}.{}, please check the event:[" + eventId + "]", event.getApiClass(), event.getApiMethod(), event.getId(), ex);
            handleException(event, ex, method);
        } finally {
            ConsistentEventReplayHolder.removeCurrentEventId();
            ConsistentEventReplayLock.release(eventId);
        }

    }

    @Override
    public void cleanEvents() {
        Stopwatch sw = Stopwatch.createStarted();
        while (true) {
            // 分页查询待清理事件记录ID列表
            List<Long> list = consistentEventService.getNeedCleanEvents(appName, dataCenterConfig.getCenterId(), config.getRetainDays(), config.getCleanPageSize());
            if (CollectionUtils.isEmpty(list)) {
                break;
            }
            // 分页批量删除
            consistentEventService.cleanEvents(list);
        }
        LOGGER.info("Finish clean consistent events, cost:[{}].", sw.stop().toString());
    }

    @Override
    public List<ConsistentEvent> getConsistentEvents(List<Long> eventIds) {
        return consistentEventService.getConsistentEvents(appName, eventIds);
    }

    /**
     * 处理异常情况
     * @param event
     * @param ex
     * @param method
     */
    private void handleException(ConsistentEvent event, Throwable ex, Method method) {
        String exception = ex.getClass().getName() + ":" + ex.getMessage();
        Long eventId = event.getId();
        EventuallyConsistent annotation = method.getAnnotation(EventuallyConsistent.class);

        // 判断串行执行情况下，根据指定的异常对当前串行执行队列进行中断，保证串行执行顺序。
        if (!event.isConcurrency()) {
            // 判断interruptFor指定异常，中断串行重放
            Class<? extends Throwable>[] interruptFor = annotation.interruptFor();
            if (ArrayUtils.contains(interruptFor, ex.getClass()) || ex instanceof RuntimeException) {
                consistentEventService.updateProcessing(eventId, event.getVersion(), exception);
                throw new SerialReplayInterruptedException("The serial replay interrupted, please check the event:[" + eventId + "]", ex);
            }
        }

        // 判断retryFor指定异常，更新事件状态为处理中
        Class<? extends Throwable>[] retryFor = annotation.retryFor();
        if (ArrayUtils.contains(retryFor, ex.getClass()) || ex instanceof RuntimeException) {
            consistentEventService.updateProcessing(eventId, event.getVersion(), exception);
        } else if (ex instanceof IOException || ex instanceof RestClientException || StringUtils.contains(ex.getClass().getName().toLowerCase(), TIMEOUT)) {
            // 如果是超时异常或IO异常，或者rest异常，则更新事件记录为未知状态
            consistentEventService.updateUnknown(eventId, event.getVersion(), exception);
        } else {
            consistentEventService.updateFailed(eventId, event.getVersion(), exception);
        }
    }

}
