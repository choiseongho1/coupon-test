package com.coupon.exception;

/**
 * Redis 작업 중 발생하는 예외를 나타내는 클래스입니다.
 */
public class RedisOperationException extends RuntimeException {

    public RedisOperationException(String message) {
        super(message);
    }

    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisOperationException(Throwable cause) {
        super(cause);
    }
}
