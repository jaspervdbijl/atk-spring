package com.acutus.atk.spring.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.logging.Level;

/**
 * Created by jaspervdb on 2016/05/24.
 */
@ControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity handleJwtException(HttpException ex) {
        log.warn(ex.toString());
        return new ResponseEntity(ex.getMessage(), ex.getStatus());
    }

}
