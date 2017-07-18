package com.kiselev.reflection.ui.impl.bytecode.utils;

import com.kiselev.reflection.ui.exception.ByteCodeParserException;
import com.kiselev.reflection.ui.exception.file.ReadFileException;
import com.kiselev.reflection.ui.impl.bytecode.assembly.build.constant.Constants;
import com.kiselev.reflection.ui.impl.bytecode.configuration.StateManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Created by by Aleksei Makarov on 06/26/2017.
 */
public class InnerClassesCollector {

    public static Collection<Class<?>> getInnerClasses(Class<?> clazz) {
        Set<Class<?>> innerClasses = new HashSet<>();

        // Anonymous and synthetic classes
        if (StateManager.getConfiguration().isDecompileAnonymousClasses()) {
            innerClasses.addAll(getAnonymousOrSyntheticClasses(clazz));
        }

        // Inner and nested classes
        if (StateManager.getConfiguration().isDecompileInnerAndNestedClasses()) {
            innerClasses.addAll(getInnerAndNestedClasses(clazz));
        }

        // Local classes
        if (StateManager.getConfiguration().isDecompileLocalClasses()) {
            innerClasses.addAll(getLocalClasses(clazz));
        }

        return new ArrayList<>(innerClasses);
    }

    private static Collection<Class<?>> getAnonymousOrSyntheticClasses(Class<?> clazz) {
        Collection<Class<?>> anonymousOrSyntheticClasses = new ArrayList<>();

        int classId = 0;
        while (classId++ < 2 << 15) {
            try {
                Class<?> foundedClass = Class.forName(clazz.getName() + Constants.Symbols.DOLLAR + classId);
                anonymousOrSyntheticClasses.add(foundedClass);

                if (StateManager.getConfiguration().isDecompileInnerAndNestedClasses()) {
                    anonymousOrSyntheticClasses.addAll(getInnerClasses(foundedClass));
                }

                if (StateManager.getConfiguration().isDecompileLocalClasses()) {
                    anonymousOrSyntheticClasses.addAll(getLocalClasses(foundedClass));
                }
            } catch (Exception exception) {
                break;
            }
        }

        return anonymousOrSyntheticClasses;
    }

    private static Collection<Class<?>> getInnerAndNestedClasses(Class<?> clazz) {
        Collection<Class<?>> innerAndNestedClasses = new ArrayList<>();

        for (Class<?> innerOrNestedClass : clazz.getDeclaredClasses()) {
            innerAndNestedClasses.add(innerOrNestedClass);
            innerAndNestedClasses.addAll(getInnerClasses(innerOrNestedClass));

            if (StateManager.getConfiguration().isDecompileLocalClasses()) {
                innerAndNestedClasses.addAll(getLocalClasses(innerOrNestedClass));
            }
        }

        return innerAndNestedClasses;
    }

    private static Collection<Class<?>> getLocalClasses(Class<?> clazz) {
        Collection<Class<?>> localClasses = new ArrayList<>();

        if (ClassFileUtils.isDynamicCreateClass(clazz)) {
            localClasses.addAll(collectLocalDynamicClass(clazz));
        } else {
            if (ClassFileUtils.isArchive(ClassFileUtils.getFilePath(clazz))) {
                localClasses.addAll(collectLocalClassesFromArchive(clazz));
            } else {
                localClasses.addAll(collectLocalClassesFromDirectory(clazz));
            }
        }

        return localClasses;
    }

    private static Collection<Class<?>> collectLocalDynamicClass(Class<?> clazz) {
        Collection<Class<?>> localClasses = new ArrayList<>();

        ClassLoader classLoader = clazz.getClassLoader();
        Collection<Class<?>> classes = getLoadedClasses(classLoader);
        for (Class<?> loadedClass : classes) {
            String className = ClassNameUtils.getSimpleName(loadedClass);
            if (isLocalClass(clazz, className)) {
                addLocalClass(loadedClass, localClasses);
            }
        }

        return localClasses;
    }

    private static Collection<Class<?>> collectLocalClassesFromArchive(Class<?> clazz) {
        Collection<Class<?>> localClasses = new ArrayList<>();

        String path = ClassFileUtils.getFilePath(clazz);
        File file = new File(ClassFileUtils.getArchivePath(path));
        try {
            JarFile jarFile = new JarFile(file.getPath());
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                addLocalClass(collectLocalStaticClass(clazz, entry.getName()), localClasses);
            }
        } catch (IOException exception) {
            throw new ReadFileException("Can't read jar file: " + file.getPath(), exception);
        }

        return localClasses;
    }

    private static Collection<Class<?>> collectLocalClassesFromDirectory(Class<?> clazz) {
        Collection<Class<?>> localClasses = new ArrayList<>();

        File file = new File(ClassFileUtils.getClassPackagePath(clazz));
        File[] classes = file.listFiles();

        if (classes != null) {
            for (File classFile : classes) {
                String name = ClassNameUtils.getPackageName(clazz) + Constants.Symbols.DOT + classFile.getName();
                addLocalClass(collectLocalStaticClass(clazz, name), localClasses);
            }
        }

        return localClasses;
    }

    private static void addLocalClass(Class<?> localClass, Collection<Class<?>> localClasses) {
        if (localClass != null) {
            localClasses.add(localClass);
            localClasses.addAll(getLocalClasses(localClass));

            if (StateManager.getConfiguration().isDecompileInnerAndNestedClasses()) {
                localClasses.addAll(getInnerAndNestedClasses(localClass));
            }

            if (StateManager.getConfiguration().isDecompileAnonymousClasses()) {
                localClasses.addAll(getAnonymousOrSyntheticClasses(localClass));
            }
        }
    }

    private static Class<?> collectLocalStaticClass(Class<?> clazz, String name) {
        String fullName = ClassNameUtils.normalizeFullName(name);
        String simpleName = ClassNameUtils.normalizeSimpleName(name);

        if (name.endsWith(Constants.Suffix.CLASS_FILE_SUFFIX) && isLocalClass(clazz, simpleName)) {
            try {
                return Class.forName(fullName);
            } catch (ClassNotFoundException exception) {
                throw new ByteCodeParserException("Can't load local class: " + fullName, exception);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Class<?>> getLoadedClasses(ClassLoader classLoader) {
        try {
            Field classes = ClassLoader.class.getDeclaredField("classes");
            classes.setAccessible(true);
            return (Collection<Class<?>>) classes.get(classLoader);
        } catch (ReflectiveOperationException exception) {
            throw new ByteCodeParserException("Can't get load classes", exception);
        }
    }

    private static boolean isLocalClass(Class<?> clazz, String className) {
        String name = ClassNameUtils.getSimpleName(clazz);
        return Pattern.compile(name + "\\$[\\d]+.*").matcher(className).matches()
                && !isNumber(className.replace(name + Constants.Symbols.DOLLAR, ""))
                && !className.replace(name + Constants.Symbols.DOLLAR, "")
                .contains(Constants.Symbols.DOLLAR);
    }

    private static boolean isNumber(String line) {
        return Pattern.compile("\\d+").matcher(line).matches();
    }
}
