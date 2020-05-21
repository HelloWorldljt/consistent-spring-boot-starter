package com.xiangshang.consistent.extension.context;

/**
 * 将需要重放的事件记录ID放在当前线程变量中，接口重放执行注解拦截时，判断本地线程变量中是否有事件ID，如果有，表示是接口重放，直接调用目标方法
 * 
 * @author chenrg
 * @date 2018年12月5日
 */
public class ConsistentEventReplayHolder {

	/**
	 * 保存当前方法拦截的事件记录ID，接口重放时检查是否存在，如果存在，直接调用目标接口方法
	 */
	private static final ThreadLocal<Long> EVENT_THREAD_LOCAL = new ThreadLocal<>();

	/**
	 * 获取当前事件记录ID
	 * 
	 * @return
	 */
	public static Long getCurrentEventId() {
		return EVENT_THREAD_LOCAL.get();
	}

	/**
	 * 设置当前接口重放事件ID
	 * 
	 * @param id
	 */
	public static void setCurrentEventId(Long id) {
		EVENT_THREAD_LOCAL.set(id);
	}

	/**
	 * 移除当前接口重放事件ID
	 */
	public static void removeCurrentEventId() {
		EVENT_THREAD_LOCAL.remove();
	}

}
