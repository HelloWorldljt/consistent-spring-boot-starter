package com.xiangshang.consistent.domain.controller;

import com.xiangshang.consistent.domain.business.ConsistentEventBusiness;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author chenrg
 * Created at 2019/8/15 17:19
 **/
@RestController
@RequestMapping("/consistent/endpoint")
public class ConsistentController {

    @Autowired
    private ConsistentEventBusiness consistentEventBusiness;

    /**
     * 根据最终一致性事件ID列表查询事件详情
     *
     * @param eventIds
     * @return
     */
    @ResponseBody
    @RequestMapping("/queryConsistents")
    public List<ConsistentEvent> queryConsistentDetails(@RequestParam List<Long> eventIds) {
        return consistentEventBusiness.getConsistentEvents(eventIds);
    }
}
