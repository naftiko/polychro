/**
 * Copyright 2026 Naftiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.polychro.format.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Document;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Test helpers for constructing projected {@link Document} instances directly from JSON literals.
 */
final class TestDocuments {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDocuments() {
    }

    static Document markdown(String json) {
        return markdown(json, "fixture.md");
    }

    static Document markdown(String json, String sourcePath) {
        return new Document(parse(json), "markdown", sourcePath, null, Map.of());
    }

    static Document html(String json) {
        return html(json, "fixture.html");
    }

    static Document html(String json, String sourcePath) {
        return new Document(parse(json), "html", sourcePath, null, Map.of());
    }

    static Document withSourceMap(Document base, Map<String, io.polychro.spi.SourceRange> map) {
        Map<String, io.polychro.spi.SourceRange> snapshot = new LinkedHashMap<>(map);
        return new Document(base.root(), base.format(), base.sourcePath(),
                path -> snapshot.get(path), base.metadata());
    }

    private static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
