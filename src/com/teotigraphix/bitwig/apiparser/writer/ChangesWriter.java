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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.teotigraphix.bitwig.apiparser.core.APIParser;
import com.teotigraphix.bitwig.apiparser.core.IWriterConstants;
import com.teotigraphix.bitwig.apiparser.core.Version;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.writer.impl.IndentBuffer;

/**
 * @author Michael Schmalle
 * @since 1.0
 */
public class ChangesWriter {

    private IndentBuffer buffer = new IndentBuffer();

    private APIParser parser;

    public ChangesWriter(APIParser parser) {
        this.parser = parser;
    }

    public String write() {
        List<JavaClass> classes = new ArrayList<JavaClass>(parser.getBuilder().getClasses());
        Collections.sort(classes, new ClassComparator());

        List<Version> vs = new ArrayList<Version>(parser.getVersions());
        Collections.sort(vs);
        Collections.reverse(vs);

        buffer.write("<h1>Contents</h1>");

        buffer.write("<ul>");
        for (Version version : vs) {
            buffer.write("\n");
            buffer.write("<li><a href=\"#" + version.get() + "\">Bitwig Studio " + version.get()
                    + "</a></li>\n");
        }
        buffer.write("</ul>");

        for (Version version : vs) {
            buffer.write("\n");
            buffer.write("<h1 id=\"" + version.get() + "\">Bitwig Studio " + version.get()
                    + "</h1>\n");
            buffer.write("\n");

            for (JavaClass javaClass : classes) {
                if (javaClass.isInner())
                    continue;

                StringBuilder sb = new StringBuilder();
                boolean found = false;

                String extra = "";
                if (introducedIn(version, javaClass)) {
                    extra = " - New";
                }

                sb.append("\n");
                sb.append("<h2>" + javaClass.getName() + extra + "</h2>\n");
                sb.append("\n");

                sb.append("<ul>");
                List<JavaMethod> list = parser.getSinceMethods(version.get());
                List<JavaMethod> methods = new ArrayList<JavaMethod>(list);
                Collections.sort(methods, new MethodComparator());
                for (JavaMethod method : methods) {
                    if (method.getDeclaringClass().equals(javaClass)) {
                        String methodString = new ChangesMethodWriter().writeMethod(method)
                                .toString();
                        sb.append("<li>" + methodString + "</li>\n");
                        found = true;
                    }
                }
                sb.append("</ul>");

                if (found)
                    buffer.write(sb.toString());
            }
        }

        return buffer.toString();
    }

    class ClassComparator implements Comparator<JavaClass> {
        @Override
        public int compare(JavaClass e1, JavaClass e2) {
            return e1.getName().compareTo(e2.getName());
        }
    }

    class MethodComparator implements Comparator<JavaMethod> {
        @Override
        public int compare(JavaMethod e1, JavaMethod e2) {
            return e1.getName().compareTo(e2.getName());
        }
    }

    private static boolean introducedIn(Version version, JavaClass javaClass) {
        DocletTag tag = javaClass.getTagByName(IWriterConstants.TAG_SINCE);
        if (tag != null) {
            Version classVersion = new Version(tag.getValue().replace("Bitwig Studio", "").trim());
            return classVersion.compareTo(version) == 0;
        }
        return false;
    }
}
