package com.kiselev.reflection.ui.impl.value;

import com.kiselev.reflection.ui.impl.annotation.AnnotationUtils;
import com.kiselev.reflection.ui.impl.generic.GenericsUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ValueUtils {

    public String getValue(Object object, Class<?> parsedClass) {
        if (object != null) {
            Class<?> clazz = object.getClass();
            if (clazz.isArray()) {
                List<String> listValues = new ArrayList<>();
                for (Object listValue : getArrayValues(object)) {
                    listValues.add(getValue(listValue, parsedClass));
                }

                String values = String.join(", ", listValues);
                if (listValues.size() == 1) {
                    return values;
                } else {
                    return "{" + values + "}";
                }
            }

            if (clazz.isEnum()) return new GenericsUtils().resolveType(clazz, parsedClass) + "." + object;
            if (object instanceof String) return "\"" + object + "\"";
            if (object instanceof Character) return "\'" + object + "\'";
            if (object instanceof Number || object instanceof Boolean) return object.toString();
            if (object instanceof Annotation) return new AnnotationUtils().getAnnotation(Annotation.class.cast(object), parsedClass);
            if (object instanceof Class) return new GenericsUtils().resolveType(Class.class.cast(object), parsedClass) + ".class";
        }

        return null;
    }

    private List<Object> getArrayValues(Object object) {
        List<Object> objects = new ArrayList<>();
        if (object.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(object); i++) {
                objects.add(Array.get(object, i));
            }
        }
        return objects;
    }
}
