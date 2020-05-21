package com.xiangshang.consistent.extension.exception;

/**
 * 串行重放被中断异常
 * 
 * @author chenrg
 * @date 2018年12月18日
 */
public class SerialReplayInterruptedException extends RuntimeException {

	private static final long serialVersionUID = 5921697763148368588L;

	public SerialReplayInterruptedException() {
		super();
	}

	public SerialReplayInterruptedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SerialReplayInterruptedException(String message) {
		super(message);
	}

	public SerialReplayInterruptedException(Throwable cause) {
		super(cause);
	}

}
