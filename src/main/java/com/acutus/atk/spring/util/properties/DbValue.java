package com.acutus.atk.spring.util.properties;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DbValue {
    String value();
}
