package com.kiselev.reflection.ui.impl.bytecode.decompile.jd;

import com.kiselev.reflection.ui.configuration.Configuration;
import com.kiselev.reflection.ui.exception.DecompilationException;
import com.kiselev.reflection.ui.impl.bytecode.assembly.build.constant.Constants;
import com.kiselev.reflection.ui.impl.bytecode.decompile.Decompiler;
import com.kiselev.reflection.ui.impl.bytecode.decompile.jd.configuration.JDBuilderConfiguration;
import com.kiselev.reflection.ui.impl.bytecode.utils.ClassNameUtils;
import jd.common.preferences.CommonPreferences;
import jd.common.printer.text.PlainTextPrinter;
import jd.core.loader.Loader;
import jd.core.loader.LoaderException;
import jd.core.model.classfile.ClassFile;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.InstructionPrinter;
import jd.core.printer.Printer;
import jd.core.process.analyzer.classfile.ClassFileAnalyzer;
import jd.core.process.analyzer.classfile.ReferenceAnalyzer;
import jd.core.process.deserializer.ClassFileDeserializer;
import jd.core.process.deserializer.ClassFormatException;
import jd.core.process.layouter.ClassFileLayouter;
import jd.core.process.writer.ClassFileWriter;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Created by Aleksei Makarov on 07/19/2017.
 * <p>
 * This decompiler doesn't work with java 8 and can't show local classes
 */
public final class JDDecompiler implements Decompiler {

    private ArrayList<ClassFile> innerClasses = new ArrayList<>();

    private Map<String, Object> configuration;

    @Override
    public String decompile(byte[] byteCode) {
        try {
            Loader loader = new JDLoader(byteCode);
            JDPrinter jdPrinter = new JDPrinter(System.out);

            String className = ClassNameUtils.getClassName(byteCode);
            ClassFile classFile = ClassFileDeserializer.Deserialize(loader, className);
            resolveInnerClasses(classFile, innerClasses);

            ReferenceMap referenceMap = new ReferenceMap();
            ClassFileAnalyzer.Analyze(referenceMap, classFile);
            ReferenceAnalyzer.Analyze(referenceMap, classFile);

            CommonPreferences preferences = getCommonPreferences();
            Printer printer = new InstructionPrinter(new PlainTextPrinter(preferences, jdPrinter));

            ArrayList<LayoutBlock> layoutBlockList = new ArrayList<>(1024);
            int maxLineNumber = ClassFileLayouter.Layout(preferences, referenceMap, classFile, layoutBlockList);
            ClassFileWriter.Write(loader, printer, referenceMap, maxLineNumber,
                    classFile.getMajorVersion(), classFile.getMinorVersion(), layoutBlockList);

            return jdPrinter.getSource();
        } catch (ClassFormatException | NullPointerException exception) {
            throw new DecompilationException("JD can't decompile class: " + ClassNameUtils.getClassName(byteCode), exception);
        } catch (LoaderException | FileNotFoundException exception) {
            throw new DecompilationException("Decompilation process is interrupted", exception);
        }
    }

    @Override
    public void setConfiguration(Configuration configuration) {
        if (this.configuration == null) {
            this.configuration = getDefaultConfiguration();
        }
        this.configuration.putAll(configuration.getConfiguration());
    }

    @Override
    public void appendAdditionalClasses(Collection<byte[]> classes) {
        ArrayList<ClassFile> innerClasses = new ArrayList<>();
        for (byte[] bytecode : classes) {
            try {
                String className = ClassNameUtils.getClassName(bytecode);
                innerClasses.add(ClassFileDeserializer.Deserialize(new JDLoader(bytecode), className));
            } catch (LoaderException exception) {
                throw new DecompilationException("Error loading inner classes", exception);
            }
        }

        this.innerClasses = innerClasses;
    }

    private void resolveInnerClasses(ClassFile classFile, List<ClassFile> innerClasses) {
        String className = ClassNameUtils.normalizeSimpleName(classFile.getThisClassName()) + Constants.Symbols.DOLLAR;
        Iterator<ClassFile> iterator = innerClasses.iterator();
        ArrayList<ClassFile> currentInnerClasses = new ArrayList<>();
        while (iterator.hasNext()) {
            ClassFile innerClass = iterator.next();
            String innerClassName = ClassNameUtils.normalizeSimpleName(innerClass.getThisClassName());
            if (!innerClassName.replace(className, "").contains(Constants.Symbols.DOLLAR)) {
                innerClass.setOuterClass(classFile);
                currentInnerClasses.add(innerClass);
                iterator.remove();
            }
        }

        classFile.setInnerClassFiles(currentInnerClasses);

        if (!innerClasses.isEmpty()) {
            for (ClassFile currentInnerClass : currentInnerClasses) {
                resolveInnerClasses(currentInnerClass, innerClasses);
            }
        }
    }

    private CommonPreferences getCommonPreferences() {
        if (configuration == null) {
            this.configuration = getDefaultConfiguration();
        } else {
            Map<String, Object> newConfiguration = getDefaultConfiguration();
            newConfiguration.putAll(configuration);
            this.configuration = newConfiguration;
        }

        boolean showDefaultConstructor = (boolean) configuration.get("shc");
        boolean realignmentLineNumber = (boolean) configuration.get("rln");
        boolean showPrefixThis = (boolean) configuration.get("spt");
        boolean mergeEmptyLines = (boolean) configuration.get("mel");
        boolean unicodeEscape = (boolean) configuration.get("uce");
        boolean showLineNumbers = (boolean) configuration.get("sln");

        return new CommonPreferences(showDefaultConstructor,
                realignmentLineNumber,
                showPrefixThis,
                mergeEmptyLines,
                unicodeEscape,
                showLineNumbers);
    }

    private Map<String, Object> getDefaultConfiguration() {
        return JDBuilderConfiguration
                .getBuilderConfiguration()
                .showDefaultConstructor(true)
                .realignmentLineNumber(true)
                .showPrefixThis(true)
                .mergeEmptyLines(true)
                .unicodeEscape(false)
                .showLineNumbers(false)
                .setCountIndentSpaces(4)
                .getConfiguration();
    }

    private class JDLoader implements Loader {

        private byte[] bytecode;

        public JDLoader(byte[] bytecode) {
            this.bytecode = bytecode;
        }

        @Override
        public DataInputStream load(String dummy) throws LoaderException {
            return new DataInputStream(new ByteArrayInputStream(bytecode));
        }

        @Override
        public boolean canLoad(String dummy) {
            return true;
        }
    }

    private class JDPrinter extends PrintStream {

        private StringBuilder builder = new StringBuilder();

        public JDPrinter(OutputStream stream) throws FileNotFoundException {
            super(stream);  //stub
        }

        @Override
        public PrintStream append(CharSequence csq) {
            if (isContainsOpenBlock(csq)) {
                int index = getFirstNonSpaceNumber(builder);
                if (builder.charAt(index) == '\n') {
                    builder.deleteCharAt(index);
                }
            } else if (csq.equals("  ")) {
                builder.append(configuration.get("ind"));
                return null;
            } else if (csq.equals("throws") || csq.equals("implements") || csq.equals("extends")) {
                builder.deleteCharAt(getNumberOfLineSeparator(builder));
                builder.delete(getFirstNonSpaceNumber(builder), builder.length() - 1);
            }
            builder.append(csq);
            return null;
        }

        public String getSource() {
            return normalizeOpenBlockCharacter(builder);
        }

        private int getNumberOfLineSeparator(StringBuilder builder) {
            int index = builder.length() - 1;
            while (builder.charAt(index) != '\n') {
                index--;
            }

            return index + 1;
        }

        private int getFirstNonSpaceNumber(StringBuilder builder) {
            return getFirstNonSpaceNumber(builder, builder.length());
        }

        private int getFirstNonSpaceNumber(StringBuilder line, int number) {
            for (int i = number - 1; i > 0; i--) {
                if (line.charAt(i) != ' ') {
                    return i;
                }
            }

            return -1;
        }

        private boolean isContainsOpenBlock(CharSequence charSequence) {
            int index = charSequence.length() - 1;
            for (int i = 0; i < index; i++) {
                if (charSequence.charAt(index) == '{') {
                    return true;
                }
            }

            return false;
        }

        private String normalizeOpenBlockCharacter(StringBuilder builder) {
            int index = 1;
            while (index != 0) {
                int openBlock = builder.indexOf("{", index);
                int nonSpace = getFirstNonSpaceNumber(builder, openBlock);
                if (nonSpace != -1 && builder.charAt(nonSpace) == '\n') {
                    builder.delete(nonSpace, openBlock);
                    builder.insert(nonSpace, ' ');
                    index = openBlock;
                } else {
                    index = openBlock + 1;
                }
            }

            return builder.toString();
        }
    }
}
