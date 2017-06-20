package com.kiselev.reflection.ui.impl.indent;

import com.kiselev.reflection.ui.impl.imports.ManagerImportUtils;

import java.lang.reflect.Member;

public class IndentUtils {

    public String getIndent(Object object) {
        StringBuilder indent = new StringBuilder();

        Class<?> declaringClass;

        if (object instanceof Member) {

            Member member = Member.class.cast(object);
            declaringClass = member.getDeclaringClass();

            if (declaringClass != null) {
                indent.append("    ");
            }
        } else if (object instanceof Class) {
            declaringClass = Class.class.cast(object);
        } else {
            return "";
        }

        if (!ManagerImportUtils.getImportUtils().getParsedClass().equals(declaringClass)) {
            while (declaringClass != null && (declaringClass = declaringClass.getDeclaringClass()) != null) {
                indent.append("    ");
            }
        }

        return indent.toString();
    }
}
