package com.acutus.atk.spring.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HttpException extends RuntimeException {

    private HttpStatus status;
    private String message;
    private Throwable exception;

    public HttpException(HttpStatus status) {
        this.status = status;
    }

    public HttpException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
