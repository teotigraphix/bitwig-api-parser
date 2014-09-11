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

import java.util.ListIterator;

import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.writer.ModelWriter;

/**
 * @author Michael Schmalle
 * @since 1.0
 */
public class ChangesMethodWriter extends JsModelWriter {

    public ChangesMethodWriter() {
        super("");
    }

    @Override
    protected void commentHeader(JavaAnnotatedElement entity) {
        String comment = entity.getComment();
        if ((comment != null && !comment.equals("")) || (entity.getTags().size() > 0)) {
            int endIndex = comment.indexOf(".") + 1;
            if (endIndex < 1)
                endIndex = comment.length();
            comment = comment.substring(0, endIndex).trim();
            getBuffer().write(comment + "\n");
        }
    }

    @Override
    public ModelWriter writeMethod(JavaMethod method) {

        String returnType = getJsType(method.getReturns().getGenericFullyQualifiedName());
        getBuffer().write("<code>");
        getBuffer().write(method.getName());
        getBuffer().write(" (");

        for (ListIterator<JavaParameter> iter = method.getParameters().listIterator(); iter
                .hasNext();) {
            writeParameter(iter.next());
            if (iter.hasNext()) {
                getBuffer().write(", ");
            }
        }

        getBuffer().write(")");
        getBuffer().write(":");
        getBuffer().write(returnType);
        getBuffer().write("</code><br/>");

        commentHeader(method);

        return this;
    }

    @Override
    public ModelWriter writeParameter(JavaParameter parameter) {
        if (parameter.isVarArgs()) {
            getBuffer().write("/*...*/");
        }

        String type = getJsType(parameter.getType().getGenericFullyQualifiedName());
        getBuffer().write(parameter.getName());
        getBuffer().write(":");
        getBuffer().write(type);

        return this;
    }
}
