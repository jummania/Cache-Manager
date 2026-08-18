package com.jummania;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class MyProcessor extends AbstractProcessor {

    private static final Set<String> DEFAULT_IMPORTS = Set.of("com.jummania.writer.Writer", "com.jummania.reader.Reader", "java.io.IOException");
    private final Set<String> types = new HashSet<>();
    StringBuilder stringBuilder = new StringBuilder();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        SerializerBuilder serializerBuilder = new SerializerBuilder(processingEnv, stringBuilder, types);
        DeSerializer deSerializer = new DeSerializer(processingEnv, stringBuilder, types);

        for (Element element : roundEnv.getElementsAnnotatedWith(Serializable.class)) {

            if (element.getKind() == ElementKind.CLASS) {

                String packageName = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
                types.addAll(DEFAULT_IMPORTS);
                String className = element.getSimpleName().toString();
                String targetClassName = className + "_";
                stringBuilder.append("\npublic final class ").append(targetClassName).append(" {\n\n");

                types.add(element.asType().toString());
                String varName = Character.toLowerCase(className.charAt(0)) + className.substring(1);

                stringBuilder.append("    public static void serialize(").append(className).append(" ").append(varName).append(", Writer writer) throws IOException {\n");
                stringBuilder.append("        if (").append(varName).append(" == null) writer.writeByte((byte) 0);\n").append("        else {\n");
                stringBuilder.append("            writer.writeByte((byte) 1);\n");
                TypeElement typeElement = (TypeElement) element;
                serializerBuilder.write(typeElement, varName, 2, false);

                stringBuilder.append("        }\n\n");
                stringBuilder.append("    }\n\n");

                stringBuilder.append("    public static void deSerializer(Reader reader) throws IOException {\n");
                stringBuilder.append("        if (").append("reader.readByte() == 0) return;\n");
                deSerializer.read(typeElement, null, 1);

                stringBuilder.append("    }\n\n");
                stringBuilder.append("}\n");

                try {
                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "." + targetClassName, element);
                    try (Writer writer = builderFile.openWriter()) {
                        writer.write(toString(packageName));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("com.jummania.Serializable");
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("org.gradle.annotation.processing.isolating");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_17;
    }


    private String toString(String packageName) {
        StringBuilder importBuilder = new StringBuilder(packageName.length() + types.size() * 9);

        importBuilder.append("package ").append(packageName).append(";\n\n\n");
        List<String> sorted = new ArrayList<>(types);
        Collections.sort(sorted);

        for (String type : sorted) {
            importBuilder.append("import ").append(type).append(";\n");
        }

        importBuilder.append("\n");
        stringBuilder.insert(0, importBuilder);
        String result = stringBuilder.toString();

        stringBuilder.setLength(0);
        types.clear();
        return result;
    }
}
