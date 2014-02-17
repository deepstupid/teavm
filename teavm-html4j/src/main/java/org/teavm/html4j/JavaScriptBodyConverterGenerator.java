/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.html4j;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JavaScriptBodyConverterGenerator implements Generator {
    private static final String convCls = JavaScriptBodyConverter.class.getName();
    static final MethodReference intValueMethod = new MethodReference("java.lang.Integer",
            new MethodDescriptor("intValue", ValueType.INTEGER));
    private static final ValueType objType = ValueType.object("java.lang.Object");
    static final MethodReference toJsMethod = new MethodReference(convCls, new MethodDescriptor(
            "toJavaScript", objType, objType));
    static final MethodReference fromJsMethod = new MethodReference(convCls, new MethodDescriptor(
            "fromJavaScript", objType, objType));

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "toJavaScript":
                generateToJavaScript(context, writer);
                break;
            case "fromJavaScript":
                generateFromJavaScript(context, writer);
                break;
        }
    }

    private void generateToJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(1);
        writer.append("if (" + obj + " === null) {").softNewLine().indent();
        writer.append("return null;").softNewLine();
        writer.outdent().append("} else if (" + obj + ".constructor.$meta.item) {").indent().softNewLine();
        writer.append("var arr = new Array(" + obj + ".data.length);").softNewLine();
        writer.append("for (var i = 0; i < arr.length; ++i) {").indent().softNewLine();
        writer.append("arr[i] = ").appendMethodBody(toJsMethod).append("(" + obj + ".data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return arr;").softNewLine();
        writer.outdent().append("}");
        writer.append(" else if (" + obj + ".constructor === ").appendClass("java.lang.String")
                .append(") {").indent().softNewLine();
        generateStringToJavaScript(context, writer);
        writer.outdent().append("} else if (" + obj + ".constructor === ").appendClass("java.lang.Integer")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(intValueMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("}");
        writer.append(" else {").indent().softNewLine();
        writer.append("return " + obj + ";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateFromJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(1);
        writer.append("if (" + obj +" === null || " + obj + " === undefined)").ws().append("{")
                .softNewLine().indent();
        writer.append("return null;").softNewLine();
        writer.outdent().append("} else if (" + obj + " instanceof Array) {").indent().softNewLine();
        writer.append("var arr = $rt_createArray($rt_objcls(), " + obj + ".length);").softNewLine();
        writer.append("for (var i = 0; i < arr.data.length; ++i) {").indent().softNewLine();
        writer.append("arr.data[i] = ").appendMethodBody(fromJsMethod).append("(" + obj + "[i]);")
                .softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return arr;").softNewLine();
        writer.outdent().append("}");
        writer.append(" else if (" + obj + ".constructor === ").appendClass("java.lang.String")
                .append(") {").indent().softNewLine();
        writer.append("return $rt_str(" + obj + ");").softNewLine();
        writer.outdent().append("}");
        writer.ws().append("else").ws().append("{").indent().softNewLine();
        writer.append("return ").append(obj).append(";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateStringToJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        FieldReference charsField = new FieldReference("java.lang.String", "characters");
        writer.append("var result = \"\";").softNewLine();
        writer.append("var data = ").append(context.getParameterName(1)).append('.')
                .appendField(charsField).append(".data;").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }
}
