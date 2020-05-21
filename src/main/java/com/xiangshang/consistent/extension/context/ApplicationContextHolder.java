package com.xiangshang.consistent.extension.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 持有applicationContext实例，用于从容器中获取bean
 * 
 * @author chenrg
 * @date 2018年12月10日
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ApplicationContextHolder.applicationContext = applicationContext;
	}

	/**
	 * 根据类型获取容器中的实例
	 * 
	 * @param claz
	 * @return
	 */
	public static Object getBean(Class<?> claz) {
		return applicationContext.getBean(claz);
	}

}
