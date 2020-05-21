package com.xiangshang.consistent.extension.context;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * 防止多个线程同时执行同一个event事件，加上同步锁。如果在set中查询到当前事件id，则不再执行。
 *
 * @author chenrg
 * @date 2018年12月7日
 */
public class ConsistentEventReplayLock {

    public static final ConcurrentSkipListSet<Long> EVENT_REPLAY_LOCK = new ConcurrentSkipListSet<>();

    /**
     * 判断是否被锁定
     *
     * @param eventId
     * @return 如果在set中存在，表示当前正被锁定，返回true。本次不再执行
     */
    public static final boolean isLocked(Long eventId) {
        return EVENT_REPLAY_LOCK.contains(eventId);
    }

    /**
     * 如果当前set中不存在，表示该事件待重放。则加锁
     *
     * @param eventId
     */
    public static final void lock(Long eventId) {
        EVENT_REPLAY_LOCK.add(eventId);
    }

    /**
     * 当执行完本次事件重放后，移除锁
     *
     * @param eventId
     */
    public static final void release(Long eventId) {
        EVENT_REPLAY_LOCK.remove(eventId);
    }

    /**
     * 根据给定的事件ID列表，找出当前正锁定准备执行的事件ID。并从待重放事件列表中删除，当次不重放。
     *
     * @param waitToRepayEventIds
     * @return
     */
    public static final void removeLockedEventIds(List<Long> waitToRepayEventIds) {
        Iterator<Long> iterator = waitToRepayEventIds.iterator();
        while (iterator.hasNext()) {
            if (EVENT_REPLAY_LOCK.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

}
