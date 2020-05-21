package com.xiangshang.consistent.aspect;

import com.google.common.base.Stopwatch;
import com.xiangshang.consistent.annotation.EventuallyConsistent;
import com.xiangshang.consistent.annotation.SerialNumber;
import com.xiangshang.consistent.config.EventuallyConsistentConfig;
import com.xiangshang.consistent.config.EventuallyDataCenterConfig;
import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.domain.model.ApiInvocation;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import com.xiangshang.consistent.extension.context.ConsistentEventReplayHolder;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 最终一致性标签拦截
 * <p>
 *
 * @author chenrg
 * @date 2018年12月4日
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EventuallyConsistentAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventuallyConsistentAspect.class);

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private EventuallyConsistentConfig config;

    @Autowired
    private EventuallyDataCenterConfig dataCenterConfig;

    @Autowired
    private ConsistentEventBusiness consistentEventBusiness;

    @Resource(name = "eventuallyConsistentExecutor")
    private ThreadPoolTaskExecutor executor;

    @Pointcut("execution(public * *(..)) && @annotation(com.xiangshang.consistent.annotation.EventuallyConsistent)")
    public void consistentPointcut() {

    }

    /**
     * 拦截被{@link EventuallyConsistent}标记的方法。如果该方法不是重放调用，插入接口调用事件记录并直接返回，插入事件记录的事务应在当前本地事务中。
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("consistentPointcut()")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        // 判断是否为接口重放调用，如果是，直接调用目标接口
        Long replayEventId = ConsistentEventReplayHolder.getCurrentEventId();
        if (replayEventId != null) {
            LOGGER.debug("Intercept eventually consistent replay, replay event id :[{}]", replayEventId);
            return pjp.proceed();
        }

        Stopwatch sw = Stopwatch.createStarted();

        // 目标方法的签名
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // 判断是否有事务注解，如果添加事务注解，只支持REQUIRED传播且非只读
        validateTransactionals(pjp, method);

        // 构建并保存方法调用日志
        buildConsistentEvent(pjp, method);

        sw.stop();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Eventually consistent intercept cost : [{}]", sw.toString());
        }

        // 不调用目标方法，直接返回
        return null;
    }

    /**
     * 判断是否有事务注解，如果添加事务注解，只支持REQUIRED传播且非只读
     *
     * @param method
     */
    private void validateTransactionals(ProceedingJoinPoint pjp, Method method) {
        Annotation[] annotations = method.getDeclaredAnnotations();
        // 如果有事务注解，则只支持REQUIRED传播机制
        List<Annotation> transactionals = Arrays.stream(annotations).filter(anno -> anno instanceof Transactional)
                .collect(Collectors.toList());
        transactionals.stream().forEach(anno -> {
            Transactional transactional = (Transactional) anno;
            if (!Objects.equals(transactional.propagation(), Propagation.REQUIRED) || transactional.readOnly()) {
                throw new RuntimeException(String.format(
                        "Intercept eventually consistent method:[%1s.%2s], Transactional only allowed declare Propagation.REQUIRED.",
                        pjp.getTarget().getClass().getName(), method.getName()));
            }
        });
    }

    /**
     * 构建调用日志，并保存到数据库
     *
     * @param pjp
     * @param method
     */
    private void buildConsistentEvent(ProceedingJoinPoint pjp, Method method) {
        String apiClass = pjp.getTarget().getClass().getName();
        String apiMethod = method.getName();
        // 方法参数类型列表
        Class[] apiArgTypes = method.getParameterTypes();
        // 方法参数值列表
        Object[] apiArgs = pjp.getArgs();
        // 方法返回值类型
        String apiRetType = method.getReturnType().getName();
        // 获取注解属性值
        EventuallyConsistent annotation = method.getAnnotation(EventuallyConsistent.class);
        // 获取流水号
        String serialNumber = getSerialNumber(apiClass, apiMethod, method.getParameterAnnotations(), pjp.getArgs());

        ApiInvocation apiInvocation = new ApiInvocation(apiMethod, apiArgTypes, apiArgs);

        ConsistentEvent consistentEvent = new ConsistentEvent();
        consistentEvent.setSerialNumber(serialNumber);
        consistentEvent.setAppName(appName);
        consistentEvent.setApiClass(apiClass);
        consistentEvent.setApiMethod(apiMethod);
        consistentEvent.setApiInvocation(apiInvocation);
        consistentEvent.setApiRetType(apiRetType);
        consistentEvent.setConcurrency(annotation.concurrent());
        consistentEvent.setRetryInterval(annotation.retryInterval());
        consistentEvent.setCenterId(dataCenterConfig.getCenterId());
        consistentEventBusiness.saveConsistentEvent(consistentEvent);

        // 如果是串行，则跳过。让定时任务去执行，能保证串行顺序
        if (!annotation.concurrent()) {
            return;
        }

        // 如果是并行，事务提交后，立即异步执行当前的事件调用
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                try {
                    executor.execute(() -> {
                        consistentEventBusiness.replayEvent(consistentEvent);
                    });
                } catch (Throwable e) {
                    LOGGER.error("Replay consistent event immediately after transaction commit, but occured exception. id is:[{}]", consistentEvent.getId(), e);
                }
            }
        });
    }

    /**
     * 从方法参数中获取本次调用的业务流水号
     *
     * @param apiClass
     * @param apiMethod
     * @param paramAnnotations
     * @param args
     * @return
     */
    private String getSerialNumber(String apiClass, String apiMethod, Annotation[][] paramAnnotations, Object[] args) {
        Object serialNumber = null;
        serialNumber = getSerialNumberFromMethodParameters(apiClass, apiMethod, paramAnnotations, args);
        if (serialNumber == null) {
            serialNumber = getSerialNumberFromParameterFields(apiClass, apiMethod, args);
        }
        return serialNumber != null ? String.valueOf(serialNumber) : null;
    }

    /**
     * 从参数实体类属性中找出被@{@link SerialNumber}标记的属性值。返回第一个标记@{@link SerialNumber}注解的属性值。
     *
     * @param apiClass
     * @param apiMethod
     * @param args
     * @return
     */
    private Object getSerialNumberFromParameterFields(String apiClass, String apiMethod, Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return null;
        }
        Object serialNumber = null;
        SerialNumber annotation = null;
        int index = 0;
        tag:
        for (; index < args.length; index++) {
            Object param = args[index];
            if (param == null) {
                continue;
            }
            Class<?> paramClass = param.getClass();
            if (paramClass.isPrimitive()) {
                continue;
            }
            if (paramClass.isArray()) {
                continue;
            }
            Field[] fields = paramClass.getDeclaredFields();
            if (ArrayUtils.isEmpty(fields)) {
                continue;
            }
            for (Field field : fields) {
                Annotation[] fieldAnns = field.getDeclaredAnnotations();
                if (ArrayUtils.isEmpty(fieldAnns)) {
                    continue;
                }
                annotation = (SerialNumber) Arrays.stream(fieldAnns).filter(anno -> anno instanceof SerialNumber).findFirst().orElse(null);
                if (annotation == null) {
                    continue;
                }
                ReflectionUtils.makeAccessible(field);
                serialNumber = ReflectionUtils.getField(field, param);
                break tag;
            }
        }

        checkSerialNumber(annotation, serialNumber, apiClass, apiMethod, index);

        return serialNumber;
    }

    /**
     * 从参数列表中获取被标记为@{@link SerialNumber}
     * 注解的参数值。返回第一个标记@{@link SerialNumber}注解的参数值。
     *
     * @param apiClass
     * @param apiMethod
     * @param paramAnnotations
     * @param args
     * @return
     */
    private Object getSerialNumberFromMethodParameters(String apiClass, String apiMethod,
                                                       Annotation[][] paramAnnotations, Object[] args) {
        if (ArrayUtils.isEmpty(paramAnnotations)) {
            return null;
        }
        Object serialNumber = null;
        SerialNumber annotation = null;
        int index = 0;
        for (; index < paramAnnotations.length; index++) {
            Annotation[] anns = paramAnnotations[index];
            if (ArrayUtils.isEmpty(anns)) {
                continue;
            }
            annotation = (SerialNumber) Arrays.stream(anns).filter(anno -> anno instanceof SerialNumber).findFirst().orElse(null);
            if (annotation != null) {
                serialNumber = args[index];
                break;
            }
        }

        checkSerialNumber(annotation, serialNumber, apiClass, apiMethod, index);

        return serialNumber;
    }

    /**
     * 校验serialNumber值是否必输
     *
     * @param annotation
     * @param serialNumber
     * @param apiClass
     * @param apiMethod
     * @param index
     */
    private void checkSerialNumber(SerialNumber annotation, Object serialNumber, String apiClass, String apiMethod,
                                   int index) {
        if (annotation == null) {
            return;
        }
        if (annotation.required() && serialNumber == null) {
            throw new RuntimeException(String.format(
                    "The interface [%1s.%2s] constains serialNumber can't be null, parameter index [%3s].", apiClass,
                    apiMethod, index));
        }
    }

}
