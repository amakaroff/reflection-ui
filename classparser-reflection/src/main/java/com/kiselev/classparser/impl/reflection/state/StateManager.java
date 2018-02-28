package com.kiselev.classparser.impl.reflection.state;

import com.kiselev.classparser.exception.ReflectionParserException;
import com.kiselev.classparser.impl.reflection.configuration.ConfigurationManager;
import com.kiselev.classparser.impl.reflection.parser.imports.ImportParser;

import java.util.Map;

/**
 * Created by Aleksei Makarov on 06/18/2017.
 */
public class StateManager {

    private static ThreadLocal<State> states = new ThreadLocal<>();

    public static void registerImportUtils(Class<?> clazz) {
        State state = states.get();
        if (!clazz.isMemberClass() ||
                state == null ||
                state.getImportParser() == null ||
                state.getCurrentParsedClass() == null) {
            state = new State(new ImportParser(), clazz, clazz);
            states.set(state);
            if (state.getConfigurationManager() == null) {
                state.setConfigurationManager(new ConfigurationManager());
            }
        }
    }

    public static ImportParser getImportUtils() {
        State state = states.get();
        ImportParser importParser = state.getImportParser();
        if (importParser == null) {
            throw new ReflectionParserException("Import utils is not register");
        }

        return importParser;
    }

    public static void clearState() {
        states.get().clearState();
    }

    public static Class<?> getParsedClass() {
        return states.get().getMainParsedClass();
    }

    public static Class<?> getCurrentClass() {
        return states.get().getCurrentParsedClass();
    }

    public static void setCurrentClass(Class<?> currentClass) {
        states.get().setCurrentParsedClass(currentClass);
    }

    public static void popCurrentClass() {
        State state = states.get();
        if (state.getCurrentParsedClass() != null) {
            state.setCurrentParsedClass(state.getCurrentParsedClass().getDeclaringClass());
        }
    }

    public static ConfigurationManager getConfiguration() {
        ConfigurationManager configurationManager = states.get().getConfigurationManager();
        if (configurationManager == null) {
            configurationManager = new ConfigurationManager();
            states.get().setConfigurationManager(configurationManager);
        }

        return configurationManager;
    }

    public static void setConfiguration(Map<String, Object> configuration) {
        states.get().setConfigurationManager(new ConfigurationManager(configuration));
    }
}
