package com.xiangshang.consistent.config;

/**
 * 数据中心配置
 *
 * @author chenrg
 * Created at 2019/8/30 14:12
 **/
public class EventuallyDataCenterConfig {

    /**
     * 数据中心标识
     */
    private String centerId;

    public String getCenterId() {
        return centerId;
    }

    public void setCenterId(String centerId) {
        this.centerId = centerId;
    }
}
