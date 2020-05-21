package com.xiangshang.consistent.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记支持最终一致性的方法中某个参数，该参数是调用接口的业务流水号。在中间件中会将该流水号存到事件记录中，以便追踪交易。
 *
 * @author chenrg
 * @date 2018年12月10日
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SerialNumber {

    /**
     * 是否必须，默认为true
     *
     * @return
     */
    public boolean required() default true;

}
