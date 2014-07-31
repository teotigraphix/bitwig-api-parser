////////////////////////////////////////////////////////////////////////////////
// Copyright 2014 Michael Schmalle - Teoti Graphix, LLC
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0 
// 
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and 
// limitations under the License
// 
// Author: Michael Schmalle, Principal Architect
// mschmalle at teotigraphix dot com
////////////////////////////////////////////////////////////////////////////////

package com.teotigraphix.bitwig.apiparser.writer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.teotigraphix.bitwig.apiparser.core.IWriterConstants;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaConstructor;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaInitializer;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.expression.Expression;
import com.thoughtworks.qdox.writer.ModelWriter;
import com.thoughtworks.qdox.writer.impl.IndentBuffer;

public class JsModelWriter implements ModelWriter {

    private IndentBuffer buffer = new IndentBuffer();

    private int enumFieldCounter;

    private String version;

    public JsModelWriter(String version) {
        this.version = version;
    }

    @Override
    public ModelWriter writeSource(JavaSource source) {

        writePackage(source.getPackage());

        // classes
        for (ListIterator<JavaClass> iter = source.getClasses().listIterator(); iter.hasNext();) {
            JavaClass cls = iter.next();
            writeClass(cls);
            if (iter.hasNext()) {
                buffer.newline();
            }
        }
        return this;
    }

    @Override
    public ModelWriter writePackage(JavaPackage pckg) {
        buffer.write("/* API Version - ");
        buffer.write(version);
        buffer.write(" */");
        buffer.newline();
        return this;
    }

    @Override
    public ModelWriter writeClass(JavaClass cls) {
        buffer.newline();
        commentHeader(cls);

        if (!cls.isEnum()) {
            buffer.write("function ");
            buffer.write(cls.getName());
            buffer.write("() {}");
            buffer.newline();

            List<JavaClass> implementz = cls.getImplementedInterfaces();
            // subclass
            if (implementz != null && implementz.size() > 0) {

                buffer.newline();
                buffer.write(cls.getName());
                buffer.write(".prototype = new ");
                buffer.write(implementz.get(0).getName());
                buffer.write("();");
                buffer.newline();

                buffer.write(cls.getName());
                buffer.write(".prototype.constructor = ");
                buffer.write(cls.getName());
                buffer.write(";");
                buffer.newline();
            }
        } else if (cls.isEnum()) {
            buffer.write(toSimpleName(cls.getGenericFullyQualifiedName()));
            buffer.write(" = {");
            buffer.indent();
            writeClassBody(cls);
            buffer.deindent();
            buffer.newline();
            buffer.write("};");

            return this;
        }

        return writeClassBody(cls);
    }

    private ModelWriter writeClassBody(JavaClass cls) {

        if (cls.isEnum()) {
            enumFieldCounter = 0;
            for (JavaField javaField : cls.getFields()) {
                buffer.newline();
                writeField(javaField);
                enumFieldCounter++;
            }
        }

        // extra code for specific classes
        writeExtra(cls);

        // methods
        for (JavaMethod javaMethod : cls.getMethods()) {
            buffer.newline();
            writeMethod(javaMethod);
            buffer.newline();
        }

        // enums
        for (JavaClass innerCls : cls.getNestedClasses()) {
            buffer.newline();
            writeClass(innerCls);
        }

        return this;
    }

    private void writeExtra(JavaClass cls) {
        if (cls.getName().equals("Host")) {
            buffer.newline();
            buffer.write("/**\n");
            buffer.write(" * An interface representing the host application to the script.\n");
            buffer.write(" * @global\n");
            buffer.write(" * @type {Host}\n");
            buffer.write(" */\n");
            buffer.write("var host = new Host();\n");
        }
    }

    @Override
    public ModelWriter writeInitializer(JavaInitializer arg0) {
        return null;
    }

    @Override
    public ModelWriter writeAnnotation(JavaAnnotation arg0) {
        return null;
    }

    @Override
    public ModelWriter writeConstructor(JavaConstructor arg0) {
        return null;
    }

    @Override
    public ModelWriter writeMethod(JavaMethod method) {
        commentHeader(method);

        String className = method.getDeclaringClass().getName();
        buffer.write(className + ".prototype." + method.getName() + " = function");
        buffer.write("(");

        for (ListIterator<JavaParameter> iter = method.getParameters().listIterator(); iter
                .hasNext();) {
            writeParameter(iter.next());
            if (iter.hasNext()) {
                buffer.write(", ");
            }
        }

        buffer.write(") {};");

        return this;
    }

    @Override
    public ModelWriter writeParameter(JavaParameter parameter) {
        commentHeader(parameter);
        if (parameter.isVarArgs()) {
            buffer.write("/*...*/");
        }
        buffer.write(parameter.getName());
        return this;
    }

    @Override
    public ModelWriter writeField(JavaField field) {
        commentHeader(field);

        if (!field.isEnumConstant()) {
            buffer.write(field.getType().getGenericCanonicalName());
            buffer.write(' ');
        }
        buffer.write(field.getName());

        if (field.isEnumConstant()) {
            if (field.getEnumConstantArguments() != null
                    && !field.getEnumConstantArguments().isEmpty()) {
                buffer.write("( ");
                for (Iterator<Expression> iter = field.getEnumConstantArguments().listIterator(); iter
                        .hasNext();) {
                    buffer.write(iter.next().getParameterValue().toString());
                    if (iter.hasNext()) {
                        buffer.write(", ");
                    }
                }
                buffer.write(" )");
            }
            if (field.getEnumConstantClass() != null) {
                writeClassBody(field.getEnumConstantClass());
            }
            buffer.write(": ");
            buffer.write(Integer.toString(enumFieldCounter));
            buffer.write(",");
        }

        if (field.getInitializationExpression() != null
                && field.getInitializationExpression().length() > 0) {
            {
                buffer.write(" : ");
            }
            buffer.write(field.getInitializationExpression());
        }
        return this;
    }

    protected void commentHeader(JavaAnnotatedElement entity) {
        if ((entity.getComment() != null && !entity.getComment().equals(""))
                || (entity.getTags().size() > 0)) {
            buffer.write("/**");
            buffer.newline();

            if (entity.getComment() != null && entity.getComment().length() > 0) {
                buffer.write(" * ");

                buffer.write(entity.getComment().replaceAll(IWriterConstants.NL,
                        IWriterConstants.NL + " * "));

                buffer.newline();
            }

            if (entity.getTags().size() > 0) {
                if (entity.getComment() != null && entity.getComment().length() > 0) {
                    buffer.write(" *");
                    buffer.newline();
                }
                for (DocletTag docletTag : entity.getTags()) {
                    // TODO Bug?
                    if (docletTag == null)
                        continue;

                    buffer.write(" * @");
                    try {
                        buffer.write(docletTag.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (docletTag.getName().equals(IWriterConstants.TAG_PARAM)) {
                        prependType((JavaMethod)entity, docletTag.getValue());
                    } else if (docletTag.getName().equals(IWriterConstants.TAG_RETURN)) {
                        prependReturnType((JavaMethod)entity, docletTag.getValue());
                    }

                    if (docletTag.getValue().length() > 0) {
                        buffer.write(' ');
                        buffer.write(docletTag.getValue());
                    }
                    buffer.newline();
                }
            }

            buffer.write(" */");
            buffer.newline();
        }
        //        if (entity.getAnnotations() != null) {
        //            for (JavaAnnotation annotation : entity.getAnnotations()) {
        //                writeAnnotation(annotation);
        //            }
        //        }
    }

    private void prependReturnType(JavaMethod entity, String value) {
        buffer.write(" {");
        buffer.write(getJsType(entity.getReturns().getGenericFullyQualifiedName()));
        buffer.write("}");
    }

    private void prependType(JavaMethod method, String description) {
        String[] split = description.split(" ");
        String name = split[0];
        JavaParameter parameter = method.getParameterByName(name);
        if (parameter != null && parameter.getType() != null) {
            buffer.write(" {");
            buffer.write(getJsType(parameter.getType().getGenericFullyQualifiedName()));
            buffer.write("}");
        }
    }

    private String getJsType(String genericFullName) {
        Map<String, String> typeConversions = new HashMap<String, String>();

        typeConversions.put("int", "int");
        typeConversions.put("double", "double");
        typeConversions.put("boolean", "boolean");

        typeConversions.put("java.lang.Number", "number");
        typeConversions.put("java.lang.Object", "Object");
        typeConversions.put("java.lang.String", "string");

        typeConversions.put("byte[]", "byte[]");
        typeConversions.put("java.lang.Object[]", "Object[]");
        typeConversions.put("java.lang.String[]", "String[]");

        typeConversions.put("org.mozilla.javascript.Callable", "function");
        typeConversions.put("org.mozilla.javascript.Function", "function");

        if (typeConversions.containsKey(genericFullName)) {
            String name = typeConversions.get(genericFullName);
            return name;
        }
        return toSimpleName(genericFullName);
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    private static String toSimpleName(String qualifiedName) {
        return qualifiedName.replace("com.bitwig.base.control_surface.iface.", "")
                .replace("$", ".");
    }
}
