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

package com.teotigraphix.bitwig.apiparser.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.teotigraphix.bitwig.apiparser.writer.ChangesWriter;
import com.teotigraphix.bitwig.apiparser.writer.JsModelWriter;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.impl.DefaultDocletTag;
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod;

/**
 * @author Michael Schmalle
 * @since 1.0
 */
public class APIParser {

    private List<Version> versions = new ArrayList<Version>();

    private Map<String, List<JavaMethod>> sinceMethods = new HashMap<String, List<JavaMethod>>();

    private JavaProjectBuilder builder;

    private ParserConfig config;

    public List<Version> getVersions() {
        return versions;
    }

    public JavaProjectBuilder getBuilder() {
        return builder;
    }

    public List<JavaMethod> getSinceMethods(String key) {
        return sinceMethods.get(key);
    }

    public APIParser(ParserConfig config) {
        this.config = config;
        builder = new JavaProjectBuilder();
        builder.addSourceTree(config.getSourceDirectory());
    }

    public void parse() {
        analyzeDocComments(builder.getSources());
    }

    public void build() throws IOException {
        Collection<JavaClass> classes = builder.getClasses();

        buildAPIStubs(classes);
        buildAPIChanges(classes);
    }

    private void buildAPIStubs(Collection<JavaClass> classes) throws IOException {
        for (JavaClass javaClass : classes) {
            if (javaClass.isInner())
                continue;

            JsModelWriter writer = new JsModelWriter(config.getVersion());
            writer.writeSource(javaClass.getSource());

            String fileName = javaClass.getName();
            File target = new File(config.getOutputDirectory(), fileName + IWriterConstants.JS);
            String data = writer.toString();

            save(target, data);
        }
    }

    private void buildAPIChanges(Collection<JavaClass> classes) throws IOException {
        ChangesWriter writer = new ChangesWriter(this);

        String data = writer.write();
        save(config.getChangesFile(), data);
    }

    private void analyzeDocComments(Collection<JavaSource> sources) {
        for (JavaSource javaSource : sources) {
            DefaultJavaClass clazz = (DefaultJavaClass)javaSource.getClasses().get(0);
            for (JavaMethod method : clazz.getMethods()) {
                List<DocletTag> list = new ArrayList<DocletTag>();
                DefaultJavaMethod dmethod = (DefaultJavaMethod)method;
                // TODO Bug:
                if (dmethod.getTags() == null)
                    dmethod.setTags(new ArrayList<DocletTag>());

                analyzeParameters(dmethod, list);
                analyzeReturns(dmethod, list);
                analyzeThrows(dmethod, list);
                analyzeSince(dmethod, list);

                dmethod.setTags(list);
            }
        }
    }

    private void analyzeParameters(DefaultJavaMethod dmethod, List<DocletTag> list) {
        List<JavaParameter> parameters = dmethod.getParameters();
        List<DocletTag> params = null;

        params = dmethod.getTagsByName(IWriterConstants.TAG_PARAM);

        if (parameters.size() != params.size()) {
            for (JavaParameter parameter : parameters) {
                list.add(new DefaultDocletTag(IWriterConstants.TAG_PARAM, parameter.getName()));
            }
        } else if (params.size() > 0) {
            list.addAll(params);
        }
    }

    private void analyzeReturns(DefaultJavaMethod dmethod, List<DocletTag> list) {
        JavaClass returns = dmethod.getReturns();
        String qName = returns.getFullyQualifiedName();
        DocletTag returnTag = dmethod.getTagByName(IWriterConstants.TAG_RETURN);

        if (returnTag == null && !qName.equals("void")) {
            returnTag = new DefaultDocletTag(IWriterConstants.TAG_RETURN, "");
        }
        if (returnTag != null)
            list.add(returnTag);
    }

    private void analyzeThrows(DefaultJavaMethod dmethod, List<DocletTag> list) {
        List<JavaClass> exceptions = dmethod.getExceptions();
        List<DocletTag> throwz = dmethod.getTagsByName(IWriterConstants.TAG_THROWS);

        if (exceptions.size() != throwz.size()) {
            for (JavaClass javaClass : exceptions) {
                list.add(new DefaultDocletTag(IWriterConstants.TAG_THROWS,
                        toExceptionSimpleName(javaClass.getName())));
            }
        } else if (throwz.size() > 0) {
            list.addAll(throwz);
        }
    }

    private void analyzeSince(DefaultJavaMethod dmethod, List<DocletTag> list) {
        DocletTag tag = dmethod.getTagByName(IWriterConstants.TAG_SINCE);
        if (tag != null) {
            list.add(tag);
            addSince(dmethod, tag);
        }
    }

    private void addSince(DefaultJavaMethod method, DocletTag tag) {
        final Version version = new Version(tag.getValue().replace("Bitwig Studio", "").trim());
        List<JavaMethod> list = sinceMethods.get(version.get());
        if (list == null) {
            list = new ArrayList<JavaMethod>();
            sinceMethods.put(version.get(), list);
            versions.add(version);
        }
        list.add(method);
    }

    private static void save(File target, String data) throws IOException {
        FileUtils.write(target, data);
    }

    private static String toExceptionSimpleName(String qualifiedName) {
        return qualifiedName.replace("com.bitwig.base.control_surface.", "").replace("$", ".");
    }

}
