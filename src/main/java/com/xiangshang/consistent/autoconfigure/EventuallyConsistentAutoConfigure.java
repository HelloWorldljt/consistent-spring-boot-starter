package com.xiangshang.consistent.autoconfigure;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.xiangshang.consistent.aspect.EventuallyConsistentAspect;
import com.xiangshang.consistent.config.EventuallyConsistentConfig;
import com.xiangshang.consistent.config.EventuallyDataCenterConfig;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 最终一致性中间件自动配置
 *
 * @author chenrg
 * @date 2018年12月10日
 */
@Configuration
@EnableConfigurationProperties({EventuallyConsistentConfig.class})
@ComponentScan(basePackages = "com.xiangshang.consistent")
@MapperScan("com.xiangshang.consistent.domain.dao")
public class EventuallyConsistentAutoConfigure {

    @Bean
    public EventuallyConsistentAspect eventuallyConsistentAspect() {
        return new EventuallyConsistentAspect();
    }

    /**
     * 异步执行线程池
     */
    @Bean("eventuallyConsistentExecutor")
    public ThreadPoolTaskExecutor asyncExecutor(@Autowired EventuallyConsistentConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getThreadPool().getCorePoolSize());
        executor.setMaxPoolSize(config.getThreadPool().getMaxPoolSize());
        executor.setQueueCapacity(config.getThreadPool().getQueueCapacity());
        executor.setThreadNamePrefix("eventuallyConsistentExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public EventuallyDataCenterConfig dataCenterConfig() {
        Config configService = ConfigService.getConfig("global_config");
        String centerId = configService.getProperty("data_center_code", null);
        Assert.isTrue(StringUtils.isNotBlank(centerId), "The data_center_code global config can't be null!");
        EventuallyDataCenterConfig dataCenterConfig = new EventuallyDataCenterConfig();
        dataCenterConfig.setCenterId(centerId);
        return dataCenterConfig;
    }
}
