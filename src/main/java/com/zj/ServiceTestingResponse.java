package com.zj;

import java.io.Serializable;

public class ServiceTestingResponse<T> implements Serializable {
    private int code;

    private String message;

    private T data;

    public static <T> ServiceTestingResponse<T> build(int code, String message, T data) {
        ServiceTestingResponse<T> serviceTestingResponse = new ServiceTestingResponse<>();
        serviceTestingResponse.setCode(code);
        serviceTestingResponse.setMessage(message);
        serviceTestingResponse.setData(data);
        return serviceTestingResponse;
    }

    public static <T> ServiceTestingResponse<T> success(T data) {
        return build(0, "success", data);
    }

    public static ServiceTestingResponse<Object> failed(String message) {
        return build(999999, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
