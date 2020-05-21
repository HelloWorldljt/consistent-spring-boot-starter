package com.xiangshang.consistent.domain.model;

import java.io.Serializable;
import java.util.Date;

import com.xiangshang.consistent.domain.em.InvokeStatus;

/**
 * 最终一致性方法调用事件记录。
 * 
 * @author chenrg
 * @date 2018年12月4日
 */
public class ConsistentEvent implements Serializable {

	private static final long serialVersionUID = -7255660172433204496L;

	private Long id;

	/**
	 * 业务流水号
	 */
	private String serialNumber;

	/**
	 * 应用名称
	 */
	private String appName;

	/**
	 * 接口类名
	 */
	private String apiClass;

	/**
	 * 接口方法名
	 */
	private String apiMethod;

	/**
	 * 接口参数类型列表，json格式，需跟方法签名参数位置一致
	 */
	private String apiArgTypes;

	/**
	 * 接口参数值列表，json格式，需跟方法签名参数位置一致
	 */
	private String apiArgValues;
	
	/**
	 * 接口方法调用快照，保存调用时参数信息
	 */
	private ApiInvocation apiInvocation;

	/**
	 * 接口返回值类型
	 */
	private String apiRetType;

	/**
	 * 接口返回值，json格式，可为空
	 */
	private String apiRetValue;

	/**
	 * 是否支持并行重放
	 */
	private boolean concurrency;

	/**
	 * 接口重试时间间隔
	 */
	private int retryInterval;

	/**
	 * 调用状态
	 */
	private InvokeStatus invokeStatus;

	/**
	 * 重试次数
	 */
	private Integer retry;

	/**
	 * 服务中心标识ID，用于区分多中心数据
	 */
	private String centerId;

	/**
	 * 备注信息
	 */
	private String remark;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 更新时间
	 */
	private Date updateTime;

	/**
	 * 乐观锁
	 */
	private Integer version;

	public ConsistentEvent() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getApiClass() {
		return apiClass;
	}

	public void setApiClass(String apiClass) {
		this.apiClass = apiClass;
	}

	public String getApiMethod() {
		return apiMethod;
	}

	public void setApiMethod(String apiMethod) {
		this.apiMethod = apiMethod;
	}

	public String getApiArgTypes() {
		return apiArgTypes;
	}

	public void setApiArgTypes(String apiArgTypes) {
		this.apiArgTypes = apiArgTypes;
	}

	public String getApiArgValues() {
		return apiArgValues;
	}

	public void setApiArgValues(String apiArgValues) {
		this.apiArgValues = apiArgValues;
	}

	public ApiInvocation getApiInvocation() {
		return apiInvocation;
	}

	public void setApiInvocation(ApiInvocation apiInvocation) {
		this.apiInvocation = apiInvocation;
	}

	public String getApiRetType() {
		return apiRetType;
	}

	public void setApiRetType(String apiRetType) {
		this.apiRetType = apiRetType;
	}

	public String getApiRetValue() {
		return apiRetValue;
	}

	public void setApiRetValue(String apiRetValue) {
		this.apiRetValue = apiRetValue;
	}

	public boolean isConcurrency() {
		return concurrency;
	}

	public void setConcurrency(boolean concurrency) {
		this.concurrency = concurrency;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}

	public InvokeStatus getInvokeStatus() {
		return invokeStatus;
	}

	public void setInvokeStatus(InvokeStatus invokeStatus) {
		this.invokeStatus = invokeStatus;
	}

	public Integer getRetry() {
		return retry;
	}

	public void setRetry(Integer retry) {
		this.retry = retry;
	}

	public String getCenterId() {
		return centerId;
	}

	public void setCenterId(String centerId) {
		this.centerId = centerId;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * 返回api方法签名
	 * 
	 * @return
	 */
	public String getApiSignature() {
		return String.join(".", this.apiClass, this.apiMethod);
	}

}
