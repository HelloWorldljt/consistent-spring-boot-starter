package com.xiangshang.consistent.domain.dao;

import com.xiangshang.consistent.domain.em.InvokeStatus;
import com.xiangshang.consistent.domain.model.ConsistentEvent;
import com.xiangshang.consistent.extension.handler.SerializeTypeHandler;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 一致性调用事件记录数据操作，这里的sql语句使用注解方式，可避免集成后加载不到xml配置。
 *
 * @author chenrg
 * @date 2018年12月4日
 */
@Repository
public interface ConsistentEventMapper {

    static final String BASE_COLS = "id, serial_number, app_name, api_class, api_method, api_arg_types, api_arg_values, api_invocation, api_ret_type, " +
            "api_ret_value, concurrency, retry_interval, invoke_status, retry, center_id, remark, create_time, update_time, version";

    /**
     * 插入
     *
     * @param consistentEvent
     */
    @Insert("insert into common_consistent_event(serial_number, app_name, api_class, api_method, api_arg_types, api_arg_values, "
            + "api_invocation, api_ret_type, api_ret_value, concurrency, retry_interval, invoke_status, retry, center_id, remark, " +
            "create_time, update_time, version)"
            + "values (#{serialNumber, jdbcType=VARCHAR}, "
            + "#{appName, jdbcType=VARCHAR}, "
            + "#{apiClass, jdbcType=VARCHAR}, "
            + "#{apiMethod, jdbcType=VARCHAR}, "
            + "#{apiArgTypes, jdbcType=VARCHAR}, "
            + "#{apiArgValues, jdbcType=VARCHAR}, "
            + "#{apiInvocation, typeHandler=com.xiangshang.consistent.extension.handler.SerializeTypeHandler, jdbcType=LONGVARBINARY}, "
            + "#{apiRetType, jdbcType=VARCHAR}, "
            + "#{apiRetValue, jdbcType=VARCHAR}, "
            + "#{concurrency, jdbcType=BOOLEAN}, "
            + "#{retryInterval, jdbcType=INTEGER}, "
            + "#{invokeStatus, jdbcType=VARCHAR}, "
            + "#{retry, jdbcType=INTEGER}, "
            + "#{centerId, jdbcType=VARCHAR}, "
            + "#{remark, jdbcType=VARCHAR}, "
            + "#{createTime, jdbcType=TIMESTAMP}, "
            + "#{updateTime, jdbcType=TIMESTAMP}, "
            + "#{version, jdbcType=INTEGER})")
    void insert(ConsistentEvent consistentEvent);

    /**
     * 根据ID获取记录
     *
     * @param id
     * @return
     */
    @Select("select " + BASE_COLS + " from common_consistent_event where id = #{id}")
    @Results({@Result(id = true, column = "id", property = "id"),
            @Result(column = "serial_number", property = "serialNumber"),
            @Result(column = "app_name", property = "appName"),
            @Result(column = "api_class", property = "apiClass"),
            @Result(column = "api_method", property = "apiMethod"),
            @Result(column = "api_arg_types", property = "apiArgTypes"),
            @Result(column = "api_arg_values", property = "apiArgValues"),
            @Result(column = "api_invocation", property = "apiInvocation", typeHandler = SerializeTypeHandler.class),
            @Result(column = "api_ret_type", property = "apiRetType"),
            @Result(column = "api_ret_value", property = "apiRetValue"),
            @Result(column = "concurrency", property = "concurrency"),
            @Result(column = "retry_interval", property = "retryInterval"),
            @Result(column = "invoke_status", property = "invokeStatus"),
            @Result(column = "retry", property = "retry"),
            @Result(column = "center_id", property = "centerId"),
            @Result(column = "remark", property = "remark"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "version", property = "version")})
    ConsistentEvent get(@Param("id") Long id);

    /**
     * 分页查询需要重放调用的事件ID。
     * <p>
     * <p>
     * 1. 初始状态<br>
     * 2. 未知状态且重试次数小于最大重试次数且重试间隔时间已过<br>
     * <p>
     *
     * @param appName
     * @param centerId
     * @param retryLimit       重试次数
     * @param initStatus       初始状态
     * @param retryStatusList  需要重试的状态：处理中或未知
     * @param date             当前时间
     * @param pageSize         每次获取记录数
     * @param concurrentReplay 是否是并行重放
     * @return
     */
    @Select("select id from common_consistent_event where app_name = #{appName}"
            + " and (invoke_status=#{initStatus} or (invoke_status in (${retryStatusList}) and center_id=#{centerId} and retry < #{retryLimit}"
            + " and DATE_ADD(update_time,INTERVAL retry_interval * (1+retry) MINUTE) <= #{date}))"
            + " and concurrency = #{concurrentReplay} order by id asc limit #{pageSize}")
    List<Long> getNeedReplayEventIds(@Param("appName") String appName, @Param("centerId") String centerId, @Param("retryLimit") int retryLimit,
                                     @Param("initStatus") String initStatus, @Param("retryStatusList") String retryStatusList,
                                     @Param("date") Date date, @Param("pageSize") int pageSize,
                                     @Param("concurrentReplay") boolean concurrentReplay);

    /**
     * 分页查询需要重放调用的事件记录。包含所有属性值
     * <p>
     * <p>
     * 1. 初始状态<br>
     * 2. 未知状态且重试次数小于最大重试次数且重试间隔时间已过<br>
     * <p>
     *
     * @param appName
     * @param centerId
     * @param retryLimit
     * @param initStatus
     * @param retryStatusList
     * @param date
     * @param pageSize
     * @param concurrentReplay
     * @return
     */
    @Select("select " + BASE_COLS + " from common_consistent_event where app_name = #{appName}"
            + " and (invoke_status=#{initStatus} or (invoke_status in (${retryStatusList}) and center_id=#{centerId} and retry < #{retryLimit}"
            + " and DATE_ADD(update_time,INTERVAL retry_interval * (1+retry) MINUTE) <= #{date}))"
            + " and concurrency = #{concurrentReplay} order by id asc limit #{pageSize}")
    @Results({@Result(id = true, column = "id", property = "id"),
            @Result(column = "serial_number", property = "serialNumber"),
            @Result(column = "app_name", property = "appName"),
            @Result(column = "api_class", property = "apiClass"),
            @Result(column = "api_method", property = "apiMethod"),
            @Result(column = "api_arg_types", property = "apiArgTypes"),
            @Result(column = "api_arg_values", property = "apiArgValues"),
            @Result(column = "api_invocation", property = "apiInvocation", typeHandler = SerializeTypeHandler.class),
            @Result(column = "api_ret_type", property = "apiRetType"),
            @Result(column = "api_ret_value", property = "apiRetValue"),
            @Result(column = "concurrency", property = "concurrency"),
            @Result(column = "retry_interval", property = "retryInterval"),
            @Result(column = "invoke_status", property = "invokeStatus"),
            @Result(column = "retry", property = "retry"),
            @Result(column = "center_id", property = "centerId"),
            @Result(column = "remark", property = "remark"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "version", property = "version")})
    List<ConsistentEvent> getNeedReplayEventWithAttributes(@Param("appName") String appName, @Param("centerId") String centerId,
                                                           @Param("retryLimit") int retryLimit, @Param("initStatus") String initStatus,
                                                           @Param("retryStatusList") String retryStatusList,
                                                           @Param("date") Date date, @Param("pageSize") int pageSize,
                                                           @Param("concurrentReplay") boolean concurrentReplay);

    /**
     * 更新事件重放状态
     *
     * @param id
     * @param apiRetValue
     * @param version
     * @param status
     * @param date
     * @param remark
     */
    @Update("update common_consistent_event set " +
            "invoke_status=#{status, jdbcType=VARCHAR}, " +
            "api_ret_value = #{apiRetValue, jdbcType=VARCHAR}, " +
            "update_time=#{date, jdbcType=TIMESTAMP}, " +
            "retry=retry+1, " +
            "remark=#{remark, jdbcType=VARCHAR}, " +
            "version=version+1 " +
            "where id=#{id} " +
            "and version=#{version}")
    void updateStatus(@Param("id") Long id, @Param("apiRetValue") String apiRetValue, @Param("version") Integer version,
                      @Param("status") String status, @Param("date") Date date, @Param("remark") String remark);

    /**
     * 分页查询待删除的记录ID列表
     *
     * @param appName
     * @param centerId
     * @param successStatus
     * @param date
     * @param pageSize
     * @return
     */
    @Select("select id from common_consistent_event where create_time < #{date} and app_name = #{appName} and invoke_status = #{successStatus} " +
            "and center_id = #{centerId} order by id asc limit #{pageSize}")
    List<Long> getNeedCleanEvents(@Param("appName") String appName, @Param("centerId") String centerId, @Param("successStatus") InvokeStatus successStatus,
                                  @Param("date") Date date, @Param("pageSize") int pageSize);

    /**
     * 按id列表删除记录
     *
     * @param ids
     */
    @Delete("delete from common_consistent_event where id in (${ids})")
    void deleteByIds(@Param("ids") String ids);

    /**
     * 查询事件详情
     *
     * @param appName
     * @param eventIds
     * @return
     */
    @Select("<script>" +
            "select " + BASE_COLS + " from common_consistent_event where app_name = #{appName} " +
            "and id in <foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    @Results({@Result(id = true, column = "id", property = "id"),
            @Result(column = "serial_number", property = "serialNumber"),
            @Result(column = "app_name", property = "appName"),
            @Result(column = "api_class", property = "apiClass"),
            @Result(column = "api_method", property = "apiMethod"),
            @Result(column = "api_arg_types", property = "apiArgTypes"),
            @Result(column = "api_arg_values", property = "apiArgValues"),
            @Result(column = "api_invocation", property = "apiInvocation", typeHandler = SerializeTypeHandler.class),
            @Result(column = "api_ret_type", property = "apiRetType"),
            @Result(column = "api_ret_value", property = "apiRetValue"),
            @Result(column = "concurrency", property = "concurrency"),
            @Result(column = "retry_interval", property = "retryInterval"),
            @Result(column = "invoke_status", property = "invokeStatus"),
            @Result(column = "retry", property = "retry"),
            @Result(column = "center_id", property = "centerId"),
            @Result(column = "remark", property = "remark"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "version", property = "version")})
    List<ConsistentEvent> queryConsistentEvents(@Param("appName") String appName, @Param("eventIds") List<Long> eventIds);
}
