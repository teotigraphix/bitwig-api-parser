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
import java.util.List;

import org.apache.commons.io.FileUtils;

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

    private File sourceDirectory;

    private File outputDirectory;

    private String version;

    public APIParser(ParserConfig config) {
        this.sourceDirectory = config.getSourceDirectory();
        this.outputDirectory = config.getOutputDirectory();
        this.version = config.getVersion();
    }

    public void parse() {
        final JavaProjectBuilder builder = new JavaProjectBuilder();

        builder.addSourceTree(sourceDirectory);

        analyzeDocComments(builder.getSources());

        Collection<JavaClass> classes = builder.getClasses();
        for (JavaClass javaClass : classes) {
            if (javaClass.isInner())
                continue;

            JsModelWriter writer = new JsModelWriter(version);
            writer.writeSource(javaClass.getSource());

            String fileName = javaClass.getName();
            File target = new File(outputDirectory, fileName + IWriterConstants.JS);
            String data = writer.toString();

            try {
                save(target, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    private static void save(File target, String data) throws IOException {
        FileUtils.write(target, data);
    }

    private static String toExceptionSimpleName(String qualifiedName) {
        return qualifiedName.replace("com.bitwig.base.control_surface.", "").replace("$", ".");
    }
}
