package com.acutus.atk.spring.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Component
public class LongToLocalDateConverter implements Converter<Long, LocalDate> {
    @Override
    public LocalDate convert(Long source) {
        return source != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(source),
                TimeZone.getDefault().toZoneId()).toLocalDate() : null;
    }
}
