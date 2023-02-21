package com.acutus.atk.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TimeHelper {

    private static ZoneId defaultZoneId = ZoneId.systemDefault();

    public static Date toDate(LocalDate localDate) {
        return localDate != null ? Date.from(localDate.atStartOfDay(defaultZoneId).toInstant()) : null;
    }

    public static Date toDate(LocalDateTime localDateTime) {
        return localDateTime != null ? Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public static LocalDate toLocalDate(Date date) {
        return LocalDate.ofInstant(date.toInstant(),defaultZoneId);
    }

}
