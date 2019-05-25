package com.acutus.atk.spring.error;

import lombok.extern.java.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.logging.Level;

/**
 * Created by jaspervdb on 2016/05/24.
 */
@ControllerAdvice
@Log
public class ControllerExceptionHandler {

    @ExceptionHandler
    @ResponseBody
    public ResponseEntity handleJwtException(HttpException ex) {
        log.log(Level.INFO, ex.toString());
        return new ResponseEntity(ex.getMessage(), ex.getStatus());
    }

}
