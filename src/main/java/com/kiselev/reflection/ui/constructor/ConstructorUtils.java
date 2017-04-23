package com.kiselev.reflection.ui.constructor;

import com.kiselev.reflection.ui.annotation.AnnotationsUtils;
import com.kiselev.reflection.ui.argument.ArgumentUtils;
import com.kiselev.reflection.ui.exception.ExceptionUtils;
import com.kiselev.reflection.ui.generic.GenericsUtils;
import com.kiselev.reflection.ui.indent.IndentUtils;
import com.kiselev.reflection.ui.modifier.ModifiersUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class ConstructorUtils {

    public String getConstructors(Class<?> clazz) {
        String constructors = "";

        List<String> constructorList = new ArrayList<String>();
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            constructorList.add(getConstructor(constructor));
        }

        if (!constructorList.isEmpty()) {
            constructors += String.join("\n\n", constructorList) + "\n";
        }

        return constructors;
    }

    private String getConstructor(Constructor constructor) {
        String constructorSignature = "";

        String annotations = new AnnotationsUtils().getAnnotations(constructor);

        String indent = new IndentUtils().getIndent(constructor);

        String modifiers = new ModifiersUtils().getModifiers(constructor.getModifiers());

        String generics = new GenericsUtils().getGenerics(constructor);

        String constructorName = constructor.getDeclaringClass().getSimpleName();

        String arguments = new ArgumentUtils().getArguments(constructor);

        String exceptions = new ExceptionUtils().getExceptions(constructor);

        constructorSignature += annotations + indent + modifiers + generics + constructorName + arguments + exceptions + " {\n" + indent + "}";

        return constructorSignature;
    }
}