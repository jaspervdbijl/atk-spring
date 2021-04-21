package com.acutus.atk.spring.error;

import lombok.*;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper=false)
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
