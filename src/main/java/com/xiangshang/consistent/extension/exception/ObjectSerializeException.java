package com.xiangshang.consistent.extension.exception;

/**
 * 对象序列化异常
 *
 * @author chenrg
 * @date 2019/1/29
 */
public class ObjectSerializeException extends RuntimeException {

    private static final long serialVersionUID = -1509926134847049848L;

    public ObjectSerializeException() {
        super();
    }

    public ObjectSerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectSerializeException(String message) {
        super(message);
    }

    public ObjectSerializeException(Throwable cause) {
        super(cause);
    }
}
