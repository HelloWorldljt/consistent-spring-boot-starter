package com.xiangshang.consistent.domain.model;

import java.io.Serializable;

/**
 * 
 * 最终一致性接口方法被调用时的快照信息。使用kryo进行序列化，并存储为二进制数据。解决json反序列化泛型丢失问题
 * 
 * @author chenrg
 *
 * @date 2019年1月28日
 */
public class ApiInvocation implements Serializable {

	private static final long serialVersionUID = -736069122461589602L;

	/**
	 * 接口方法名
	 */
	private String method;

	/**
	 * 接口参数类型列表
	 */
	private Class[] argTypes;

	/**
	 * 接口参数值列表
	 */
	private Object[] argValues;

	public ApiInvocation() {
		super();
	}

	public ApiInvocation(String method, Class<?>[] argTypes, Object[] argValues) {
		super();
		this.method = method;
		this.argTypes = argTypes;
		this.argValues = argValues;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Class[] getArgTypes() {
		return argTypes;
	}

	public void setArgTypes(Class[] argTypes) {
		this.argTypes = argTypes;
	}

	public Object[] getArgValues() {
		return argValues;
	}

	public void setArgValues(Object[] argValues) {
		this.argValues = argValues;
	}

}
