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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProjectedRuleTest {

    @Test
    void evaluateShouldReceiveProjectedDocument() {
        Document document = new Document(null, "markdown", "docs/example.md");
        ProjectedRule rule = new ProjectedRule() {
            @Override
            public String name() {
                return "heading-order";
            }

            @Override
            public List<Diagnostic> evaluate(Document projectedDocument) {
                return List.of(new Diagnostic(
                        Severity.WARN,
                        name(),
                        projectedDocument.format(),
                        projectedDocument.sourcePath(),
                        null));
            }
        };

        List<Diagnostic> diagnostics = rule.evaluate(document);

        assertEquals("heading-order", rule.name());
        assertEquals(1, diagnostics.size());
        assertEquals("markdown", diagnostics.getFirst().message());
        assertEquals("docs/example.md", diagnostics.getFirst().path());
    }

    @Test
    void evaluateMayReturnEmptyDiagnostics() {
        ProjectedRule rule = new ProjectedRule() {
            @Override
            public String name() {
                return "no-op";
            }

            @Override
            public List<Diagnostic> evaluate(Document document) {
                return List.of();
            }
        };

        List<Diagnostic> diagnostics = rule.evaluate(new Document(null, "html", "index.html"));

        assertSame(List.of(), diagnostics);
    }
}
