/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.model.FieldReference;
import org.teavm.platform.metadata.StaticFieldResource;

/**
 *
 * @author Alexey Andreev
 */
class BuildTimeStaticFieldResource implements StaticFieldResource, ResourceWriter {
    private FieldReference field;

    public BuildTimeStaticFieldResource(FieldReference field) {
        this.field = field;
    }

    public FieldReference getField() {
        return field;
    }

    @Override
    public void write(SourceWriter writer) throws IOException {
        writer.appendField(field);
    }
}
