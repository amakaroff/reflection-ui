package com.kiselev.reflection.ui.impl.bytecode.decompile.fernflower;

import com.kiselev.reflection.ui.impl.bytecode.assembly.build.constant.Constants;
import com.kiselev.reflection.ui.impl.bytecode.decompile.Decompiler;
import com.kiselev.reflection.ui.impl.bytecode.decompile.configuration.Configuration;
import com.kiselev.reflection.ui.impl.bytecode.decompile.fernflower.configuration.DecompilerConfiguration;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.struct.ContextUnit;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.lazy.LazyLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.Manifest;

/**
 * Created by Aleksei Makarov on 06/26/2017.
 */
public class FernflowerDecompiler implements IBytecodeProvider, IResultSaver, Decompiler {

    private byte[] byteCode;

    private String source = "";

    private Map<String, Object> configuration;

    private List<byte[]> additionalClasses = new ArrayList<>();

    public FernflowerDecompiler() {
    }

    @Override
    public String decompile(byte[] byteCode) {
        this.byteCode = byteCode;

        if (configuration == null) {
            configuration = getDefaultConfiguration();
        }

        IFernflowerLogger logger = new PrintStreamLogger(System.out);
        BaseDecompiler decompiler = new BaseDecompiler(this, this, configuration, logger);
        for (byte[] nestedClass : additionalClasses) {
            dirtyHack(decompiler, nestedClass);
        }
        dirtyHack(decompiler, byteCode);
        decompiler.decompileContext();

        return source;
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration.getConfiguration();
    }

    @Override
    public void appendAdditionalClasses(Collection<byte[]> classes) {
        this.additionalClasses.addAll(classes);
    }

    @Override
    public byte[] getBytecode(String s, String s1) throws IOException {
        return byteCode;
    }

    private Map<String, Object> getDefaultConfiguration() {
        return DecompilerConfiguration.getBuilderConfiguration().getConfiguration();
    }

    private void dirtyHack(BaseDecompiler decompiler, byte[] byteCode) {
        try {
            Fernflower fernflower = getFernflower(decompiler);
            StructClass structClass = createClassStruct(byteCode);

            StructContext structContext = fernflower.getStructContext();
            Map<String, StructClass> classes = structContext.getClasses();
            classes.put(structClass.qualifiedName, structClass);

            Map<String, ContextUnit> units = getContextUnit(structContext);

            ContextUnit unit = getFalseContextUnit(fernflower);
            unit.addClass(structClass, structClass.qualifiedName + Constants.Suffix.CLASS_FILE_SUFFIX);
            units.put(structClass.qualifiedName, unit);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private Fernflower getFernflower(BaseDecompiler decompiler) throws NoSuchFieldException, IllegalAccessException {
        Field fieldFernflower = decompiler.getClass().getDeclaredField("fernflower");
        fieldFernflower.setAccessible(true);
        return Fernflower.class.cast(fieldFernflower.get(decompiler));
    }

    private StructClass createClassStruct(byte[] byteCode) throws IOException {
        LazyLoader lazyLoader = new LazyLoader((s, s1) -> byteCode);
        StructClass structClass = new StructClass(byteCode, true, lazyLoader);
        LazyLoader.Link link = new LazyLoader.Link(1, structClass.qualifiedName, null);
        lazyLoader.addClassLink(structClass.qualifiedName, link);

        return structClass;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ContextUnit> getContextUnit(StructContext context) throws NoSuchFieldException, IllegalAccessException {
        Field fieldUnits = StructContext.class.getDeclaredField("units");
        fieldUnits.setAccessible(true);
        return (Map<String, ContextUnit>) fieldUnits.get(context);
    }

    private ContextUnit getFalseContextUnit(Fernflower fernflower) {
        return new ContextUnit(0, null, "", true, this, fernflower);
    }

    @Override
    public void saveClassFile(String s, String s1, String s2, String source, int[] ints) {
        this.source = source;
    }

    @Override
    public void createArchive(String s, String s1, Manifest manifest) {
    }

    @Override
    public void saveDirEntry(String s, String s1, String s2) {
    }

    @Override
    public void copyEntry(String s, String s1, String s2, String s3) {
    }

    @Override
    public void saveClassEntry(String s, String s1, String s2, String s3, String s4) {
    }

    @Override
    public void closeArchive(String s, String s1) {
    }

    @Override
    public void saveFolder(String s) {
    }

    @Override
    public void copyFile(String s, String s1, String s2) {
    }
}