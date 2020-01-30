package com.acutus.atk.util;

import com.acutus.atk.util.call.CallNil;
import com.acutus.atk.util.call.CallNilRet;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

public class AtkUtil {

    @SneakyThrows
    public static void handle(CallNil call) {
        call.call();
    }

    @SneakyThrows
    public static <T, R> R handle(CallNilRet<R> call) {
        return call.call();
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null && o2 == null || o1 != null && o1.equals(o2);
    }

    /**
     * return the generic type of a class
     *
     * @param clazz
     * @return
     */
    public static Class getGenericType(Class clazz, int index) {
        return ((Class) ((ParameterizedType) clazz.getGenericSuperclass())
                .getActualTypeArguments()[index]);
    }

    public static Class getGenericType(Class clazz) {
        return getGenericType(clazz, 0);
    }

    public static Class getGenericFieldType(Field field) {
        return (Class) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
    }

    /**
     * if the class is a defined primitive type
     *
     * @param type
     * @return
     */
    public static boolean isPrimitive(String type) {
        return type.startsWith("java.lang.") ||
                type.startsWith("java.time.Local");
    }

    public static boolean isPrimitive(Class type) {
        return isPrimitive(type.getName());
    }


}