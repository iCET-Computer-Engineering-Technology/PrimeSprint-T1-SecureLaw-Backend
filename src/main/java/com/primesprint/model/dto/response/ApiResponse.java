package com.primesprint.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final String code;
    private final T data;
    private final Instant timestamp;

    private ApiResponse(int status, String message, String code, T data) {
        this.status = status;
        this.message = message;
        this.code = code;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(status, message, null, data);
    }

    public static <T> ApiResponse<T> error(int status, String message, String code) {
        return new ApiResponse<>(status, message, code, null);
    }

}
