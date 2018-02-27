package com.kiselev.classparser.impl.reflection.configuration;

import com.kiselev.classparser.configuration.reflection.ReflectionBuilderConfiguration;
import com.kiselev.classparser.configuration.util.ConfigurationUtils;

import java.util.Map;

/**
 * Created by Aleksei Makarov on 07/14/2017.
 */
public class ConfigurationManager {

    private Map<String, Object> configuration;

    private ConfigurationUtils utils;

    public ConfigurationManager() {
        this.configuration = getDefaultConfiguration();
        this.utils = new ConfigurationUtils(configuration, configuration);
    }

    public ConfigurationManager(Map<String, Object> configuration) {
        this();
        this.configuration.putAll(configuration);
        this.utils.appendConfiguration(configuration);
    }

    private static Map<String, Object> getDefaultConfiguration() {
        return ReflectionBuilderConfiguration
                .configure()
                .showAnnotationTypes(true)
                .showInnerClasses(true)
                .showNonJavaModifiers(false)
                .showDefaultValueInAnnotation(false)
                .displayValueForFields(true)
                .showGenericSignatures(true)
                .showVarArgs(true)
                .enableImports(true)
                .setCountIndentSpaces(4)
                .defineLineSeparator(chooseSystemNewLineCharacter())
                .getConfiguration();
    }

    private static String chooseSystemNewLineCharacter() {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            return "\n\r";
        } else {
            return "\n";
        }
    }

    public boolean isShowAnnotationTypes() {
        return utils.getConfig("sat", Boolean.class);
    }

    public boolean isShowInnerClasses() {
        return utils.getConfig("sic", Boolean.class);
    }

    public boolean isShowNonJavaModifiers() {
        return utils.getConfig("njm", Boolean.class);
    }

    public boolean isShowDefaultValueInAnnotation() {
        return utils.getConfig("dva", Boolean.class);
    }

    public boolean isShowGenericSignatures() {
        return utils.getConfig("sgs", Boolean.class);
    }

    public boolean isShowVarArgs() {
        return utils.getConfig("sva", Boolean.class);
    }

    public boolean isDisplayFieldValue() {
        return utils.getConfig("dvf", Boolean.class);
    }

    public boolean isEnabledImports() {
        return utils.getConfig("dim", Boolean.class);
    }

    public String getIndentSpaces() {
        return utils.getConfig("cis", String.class);
    }

    public String getLineSeparator() {
        return utils.getConfig("nlc", String.class);
    }
}