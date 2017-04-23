package com.kiselev.reflection.ui.method;

import com.kiselev.reflection.ui.annotation.AnnotationsUtils;
import com.kiselev.reflection.ui.argument.ArgumentUtils;
import com.kiselev.reflection.ui.exception.ExceptionUtils;
import com.kiselev.reflection.ui.generic.GenericsUtils;
import com.kiselev.reflection.ui.indent.IndentUtils;
import com.kiselev.reflection.ui.modifier.ModifiersUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class MethodUtils {

    public String getMethods(Class<?> clazz) {
        String methods = "";

        List<String> methodList = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            methodList.add(getMethod(method));
        }

        if (!methodList.isEmpty()) {
            methods += String.join("\n\n", methodList) + "\n";
        }

        return methods;
    }

    private String getMethod(Method method) {
        String methodSignature = "";

        String annotations = new AnnotationsUtils().getAnnotations(method);

        String indent = new IndentUtils().getIndent(method);

        String isDefault = method.isDefault() ? "default " : "";

        String modifiers = new ModifiersUtils().getModifiers(method.getModifiers());

        String generics = new GenericsUtils().getGenerics(method);

        String returnType = new GenericsUtils().resolveType(method.getGenericReturnType());

        String methodName = method.getName();

        String arguments = new ArgumentUtils().getArguments(method);

        String exceptions = new ExceptionUtils().getExceptions(method);

        String body = isMethodRealization(method) ? " {\n" + indent + "}" : ";";

        methodSignature += annotations + indent + isDefault + modifiers + generics + returnType + " " + methodName + arguments + exceptions + body;

        return methodSignature;
    }

    private boolean isMethodRealization(Method method) {
        return !Modifier.isAbstract(method.getModifiers()) && !Modifier.isNative(method.getModifiers());
    }
}