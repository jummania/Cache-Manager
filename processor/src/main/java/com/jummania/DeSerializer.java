package com.jummania;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jummania.Utils.*;

class DeSerializer {
    private final Set<String> types;
    private final Set<String> names = new HashSet<>();
    private final ProcessingEnvironment processingEnv;
    private final StringBuilder builder;
    private int i = 0;

    DeSerializer(ProcessingEnvironment processingEnv, StringBuilder stringBuilder, Set<String> types) {
        this.processingEnv = processingEnv;
        this.builder = stringBuilder;
        this.types = types;
    }

    void read(TypeElement element, String parentAccessor, int spaceCount) {
        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {

                if (enclosed.getModifiers().contains(Modifier.PRIVATE)) continue;
                VariableElement field = (VariableElement) enclosed;
                String fieldName = field.getSimpleName().toString();
                TypeMirror typeMirror = field.asType();
                String fieldType = typeMirror.toString();

                if (writeAny(typeMirror, parentAccessor, fieldName, fieldType, spaceCount, null) == null) {
                    throw new RuntimeException("Unknown type for reading: " + fieldType);
                }
            }
        }
    }

    String readPrimitive(String fieldType) {
        return switch (fieldType) {
            case "int", "java.lang.Integer" -> "reader.readInt()";
            case "long", "java.lang.Long" -> "reader.readLong()";
            case "short", "java.lang.Short" -> "reader.readShort()";
            case "byte", "java.lang.Byte" -> "reader.readByte()";
            case "char", "java.lang.Character" -> "reader.readChar()";
            case "boolean", "java.lang.Boolean" -> "reader.readBoolean()";
            case "float", "java.lang.Float" -> "reader.readFloat()";
            case "double", "java.lang.Double" -> "reader.readDouble()";
            case "byte[]", "[B" -> "reader.readBytes()";
            case "java.lang.String" -> "reader.readString()";
            default -> null;
        };
    }

    boolean readPrimitive(String fieldName, String fieldType, String space) {
        switch (fieldType) {
            case "int", "java.lang.Integer" -> {
                String read = "reader.readInt();";
                builder.append(space).append("int ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "long", "java.lang.Long" -> {
                String read = "reader.readLong();";
                builder.append(space).append("long ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "short", "java.lang.Short" -> {
                String read = "reader.readShort();";
                builder.append(space).append("short ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "byte", "java.lang.Byte" -> {
                String read = "reader.readByte();";
                builder.append(space).append("byte ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "char", "java.lang.Character" -> {
                String read = "reader.readChar();";
                builder.append(space).append("char ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "boolean", "java.lang.Boolean" -> {
                String read = "reader.readBoolean();";
                builder.append(space).append("boolean ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "float", "java.lang.Float" -> {
                String read = "reader.readFloat();";
                builder.append(space).append("float ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "double", "java.lang.Double" -> {
                String read = "reader.readDouble();";
                builder.append(space).append("double ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "byte[]", "[B" -> {
                String read = "reader.readBytes();";
                builder.append(space).append("byte[] ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
            case "java.lang.String" -> {
                String read = "reader.readString();";
                builder.append(space).append("String ").append(fieldName).append(" = ").append(read).append("\n");
                return true;
            }
        }

        return false;
    }

    void readArray(TypeMirror typeMirror, String fieldName, String explicitComponentType, int spaceCount, String oldField) {
        String componentType;
        TypeMirror componentTypeMirror;

        boolean isArray = explicitComponentType != null;
        if (isArray) {
            componentType = explicitComponentType;
            componentTypeMirror = typeMirror;
        } else {
            TypeMirror[] typeArgs = getTypeArguments(typeMirror);
            componentTypeMirror = typeArgs[0];
            componentType = componentTypeMirror != null ? componentTypeMirror.toString() : "java.lang.Object";
        }

        String space = space(spaceCount++);
        String doubleSpace = space(spaceCount);
        types.add("java.util.ArrayList");
        if (isArray) {
            builder.append(removeArgument(componentTypeMirror)).append("[reader.readInt()];\n");
        } else builder.append("ArrayList(reader.readInt());\n");

        String loopName = "i" + ++i;

        builder.append(doubleSpace).append("for (int ").append(loopName).append(" = 0; ").append(loopName).append(" < ").append(fieldName).append(isArray ? ".length" : ".size()").append("; ").append(loopName).append("++) {\n");
        String readPrimitive = readPrimitive(componentType);

        if (readPrimitive == null) {
            String key;
            if ((key = writeAny(componentTypeMirror, null, fieldName, componentType, spaceCount + 1, fieldName + ".add(")) == null) {
                throw new RuntimeException("Unknown component type for reading: " + componentType);
            }
            builder.append(fieldName).append(".add(").append(key).append(");");
        } else {
            builder.append(fieldName);
            if (isArray) builder.append("[").append(loopName).append("] = ");
            else builder.append(".add(");
            builder.append(readPrimitive);
            builder.append(isArray ? ";\n" : ");\n");
        }


        //   builder.append()

        // String itemVar = getFieldName(fieldName);
        //    builder.append(space(spaceCount + 1)).append(getNormalizedTypeName(componentTypeMirror)).append(" ").append(itemVar).append(";\n");


        builder.append(space).append("}\n");
        if (oldField != null) {
            builder.append(oldField).append(fieldName).append(");\n");
        }

    }

    void writeMap(TypeMirror typeMirror, String fieldName, int spaceCount) {
        TypeMirror[] typeArgs = getTypeArguments(typeMirror);

        TypeMirror keyTypeMirror = typeArgs[0];
        TypeMirror valTypeMirror = typeArgs[1];

        String keyType = keyTypeMirror.toString();
        String valType = valTypeMirror.toString();

        String normalKey = getNormalizedTypeName(keyTypeMirror);
        String normalVal = getNormalizedTypeName(valTypeMirror);

        String entryVar = "e_" + fieldName;
        String keyVar = "k_" + fieldName;
        String valVar = "v_" + fieldName;

        names.add(valVar);

        String space = space(spaceCount++);
        String doubleSpace = space(spaceCount);
        String tripleSpace = space(spaceCount + 1);

        builder.append("HashMap(reader.readInt());\n");
        String loopName = "i" + ++i;
        builder.append("for (int ").append(loopName).append(" = 0; ").append(loopName).append(" < ").append(fieldName).append(".size(); ").append(loopName).append("++) {\n");
        // readCheck(fieldName, doubleSpace);

        types.add("java.util.HashMap");
        types.add("java.util.Map.Entry");


        String readKey = readPrimitive(keyType);
        String readValue = readPrimitive(valType);

        if (readKey != null && readValue != null) {
            builder.append(fieldName).append(".put(").append(readKey).append(", ").append(readValue).append(");\n");
        } else {

            if (readKey == null) {
                if ((readKey = writeAny(keyTypeMirror, null, keyVar, keyType, spaceCount, null)) == null) {
                    throw new RuntimeException("Unknown map key type: " + keyType);
                }
            } else {
                String tempKey = "k_" + fieldName;
                builder.append(keyType).append(" ").append(tempKey).append(" = ").append(readKey).append(";\n");
                readKey = tempKey;
            }


            if (readValue == null) {
                if ((readValue = writeAny(valTypeMirror, null, valVar, valType, spaceCount, null)) == null) {
                    throw new RuntimeException("Unknown map value type: " + valType);
                }
            } else {
                String tempValue = "v_" + fieldName;
                builder.append(valType).append(" ").append(tempValue).append(" = ").append(readValue).append(";\n");
                readValue = tempValue;
            }


            builder.append("\n").append(fieldName).append(".put(").append(readKey).append(", ").append(readValue).append(");\n");

        }


        builder.append(doubleSpace).append("}\n");
        //   builder.append(space).append("}\n");
    }

    String writeAny(TypeMirror typeMirror, String currentAccessor, String fieldName, String fieldType, int spaceCount) {

        boolean wasNull = currentAccessor == null;
        boolean isNested = oldField != null;

        if (wasNull) currentAccessor = fieldName;
        else currentAccessor = currentAccessor + fieldName;

        System.out.println(currentAccessor);

        String space = space(++spaceCount);

        if (!wasNull) {
            String read = readPrimitive(fieldType);
            if (read != null) {
                builder.append(space).append(currentAccessor).append(" = ").append(read).append(";\n");
                return fieldName;
            }
        } else if (readPrimitive(currentAccessor, fieldType, space)) return fieldName;

        String className = getNormalizedTypeName(typeMirror);

        //   if (names.contains(fieldName)) {

        if (isNested) fieldName = fieldName + i++;
        names.add(fieldName);
        builder.append("\n");
        //  }

        builder.append(space).append(className).append(" ").append(fieldName).append(" = new ");

        if (hasSerializableAnnotation(processingEnv, typeMirror)) {
            builder.append(space).append(className).append("_.").append("deSerializer(").append("reader);\n");
            return fieldName;
        }

        //  builder.append(space).append("if (").append(fieldName).append(" == null) ");

        if (isArray(typeMirror, fieldType)) {
            ArrayType arrayType = (ArrayType) typeMirror;
            TypeMirror componentTypeMirror = arrayType.getComponentType();
            String componentType = componentTypeMirror.toString();

            readArray(componentTypeMirror, fieldName, componentType, spaceCount, oldField);
            return fieldName;
        }

        if (isMap(processingEnv, typeMirror, fieldType)) {
            writeMap(typeMirror, fieldName, spaceCount);
            return fieldName;
        }

        if (isCollection(processingEnv, typeMirror, fieldType)) {
            readArray(typeMirror, fieldName, null, spaceCount, oldField);
            return fieldName;
        }

        TypeElement element = getNestedTypeElement(processingEnv, fieldType);
        if (element != null) {
            readCheck(null, space(spaceCount + 1));
            System.out.println(fieldName);
            read(element, fieldName, spaceCount);
            return fieldName;
        }

        return null;
    }

    private String getFieldName(String fieldName) {
        if (names.contains(fieldName)) {
            fieldName = fieldName + i++;
            names.add(fieldName);
        }

        return fieldName;
    }

    String space(int count) {
        return " ".repeat(count * 4);
    }

    public String getNormalizedTypeName(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return "";
        } else if (typeMirror instanceof DeclaredType declaredType) {
            TypeElement typeElement = (TypeElement) declaredType.asElement();

            String fullQualifiedName = typeElement.getQualifiedName().toString();

            if (!fullQualifiedName.startsWith("java.lang.")) {
                types.add(fullQualifiedName);
            }

            StringBuilder sb = new StringBuilder(typeElement.getSimpleName().toString());

            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                sb.append("<");
                for (int i = 0; i < typeArguments.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(getNormalizedTypeName(typeArguments.get(i)));
                }
                sb.append(">");
            }
            return sb.toString();
        } else if (typeMirror instanceof ArrayType arrayType) {
            return getNormalizedTypeName(arrayType.getComponentType()) + "[]";
        } else {
            String typeStr = typeMirror.toString();
            if (typeStr.startsWith("java.lang.")) {
                return typeStr.substring("java.lang.".length());
            }

            return typeStr;
        }
    }

    private void readCheck(String fieldName, String doubleSpace) {
        boolean isObject = fieldName == null;
        String write = isObject ? "Byte(" : "Int(";
        builder.append(doubleSpace).append("if (reader.read").append(write).append(") == 0) continue;\n");
    }


    private String removeArgument(TypeMirror typeMirror) {
        Types typeUtils = processingEnv.getTypeUtils();
        TypeMirror erasedType = typeUtils.erasure(typeMirror);
        return erasedType.toString(); // আউটপুট: "java.util.List"
    }
}
