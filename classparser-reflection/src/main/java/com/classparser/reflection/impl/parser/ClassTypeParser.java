package com.classparser.reflection.impl.parser;

public class ClassTypeParser {

    public static String getType(Class<?> clazz) {
        String type = "class ";

        if (clazz.isEnum()) {
            type = "enum ";
        } else if (clazz.isInterface()) {
            type = "interface ";
        } else if (clazz.isAnnotation()) {
            type = "@interface ";
        }

        return type;
    }
}