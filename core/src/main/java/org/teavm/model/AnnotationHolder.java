/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an annotation of Java element.
 * @author Alexey Andreev
 */
public class AnnotationHolder implements Serializable, AnnotationReader {
    private final String type;
    private final Map<String, AnnotationValue> values = new HashMap<>();

    public AnnotationHolder(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    public Map<String, AnnotationValue> getValues() {
        return values;
    }

    @Override
    public AnnotationValue getValue(String fieldName) {
        return values.get(fieldName);
    }

    @Override
    public Iterable<String> getAvailableFields() {
        return values.keySet();
    }
}
