package com.acutus.atk.spring.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Component
public class LocalDateTimeConverter implements Converter<Long, LocalDateTime> {
    @Override
    public LocalDateTime convert(Long source) {
        return source != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(source),
                TimeZone.getDefault().toZoneId()) : null;
    }
}
