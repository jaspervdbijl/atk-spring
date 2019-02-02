package com.acutus.atk.util;

public class StringUtils {

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * recursively replace
     *
     * @param string
     * @param replace
     * @param with
     * @return
     */
    public static String replaceAll(String string, String replace, String with) {
        while (string.contains(replace)) string = string.replace(replace, with);
        return string;
    }

    public static String removeAll(String string, String remove) {
        return replaceAll(string, remove, "");
    }

    public static String removeAllASpaces(String string) {
        return removeAll(string, " ");
    }

}
