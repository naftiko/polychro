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
package io.polychro.spi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Test-only {@link DocumentEnricher}, registered via
 * {@code src/test/resources/META-INF/services/io.polychro.spi.DocumentEnricher}, used to verify
 * that {@link Document#fromString(String, String, String)} discovers and delegates to a
 * ServiceLoader-registered enricher for {@code markdown} content.
 *
 * <p>Recognizes a marker prefix ({@value #MARKER}) so only the tests that opt in are affected;
 * any other markdown content falls through (returns {@code null}) and keeps exercising the
 * pre-existing raw {@code TextNode} path, so unrelated {@code DocumentTest} cases are unaffected.
 */
public class StubMarkdownEnricher implements DocumentEnricher {

    static final String MARKER = "<<stub-enrich>>";

    @Override
    public String format() {
        return "markdown";
    }

    @Override
    public Document enrich(String content, String sourcePath) {
        if (content == null || !content.startsWith(MARKER)) {
            return null;
        }
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("stub", "projected");
        SourceMap sourceMap = path -> "$.stub".equals(path) ? new SourceRange(0, 0, 0, 5) : null;
        return new Document(root, "markdown", sourcePath, sourceMap,
                Map.of("raw.content", content));
    }
}
