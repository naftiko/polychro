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
package io.polychro.core;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #20: {@link FormatGatedValidator} wraps format-specific validators so they
 * only run on documents whose {@link Document#format()} appears in the configured
 * supported set. These tests exercise every branch.
 */
class FormatGatedValidatorTest {

    @Test
    void nameShouldDelegateToWrappedValidator() {
        FormatGatedValidator gated = new FormatGatedValidator(
                new RecordingValidator("markdown"),
                Set.of("markdown"));

        assertEquals("markdown", gated.name());
    }

    @Test
    void validateShouldDelegateWhenDocumentFormatIsInSupportedSet() {
        RecordingValidator delegate = new RecordingValidator("markdown");
        FormatGatedValidator gated = new FormatGatedValidator(delegate, Set.of("markdown"));

        Document doc = new Document(new TextNode("# title"), "markdown", "doc.md");
        List<Diagnostic> result = gated.validate(doc);

        assertTrue(delegate.invoked, "delegate.validate should be invoked when format matches");
        assertEquals(1, result.size());
        assertEquals("test-diag", result.get(0).code());
    }

    @Test
    void validateShouldReturnEmptyWhenDocumentFormatIsNotInSupportedSet() {
        RecordingValidator delegate = new RecordingValidator("markdown");
        FormatGatedValidator gated = new FormatGatedValidator(delegate, Set.of("markdown"));

        Document doc = new Document(new TextNode("key: value"), "yaml", "doc.yml");
        List<Diagnostic> result = gated.validate(doc);

        assertFalse(delegate.invoked, "delegate.validate must not run for non-matching format");
        assertTrue(result.isEmpty());
    }

    @Test
    void validateShouldReturnEmptyWhenDocumentFormatIsNull() {
        RecordingValidator delegate = new RecordingValidator("markdown");
        FormatGatedValidator gated = new FormatGatedValidator(delegate, Set.of("markdown"));

        // Format is auto-detected from sourcePath when null/blank; pass a path with no
        // recognised extension so detection yields null and we exercise the null branch.
        Document doc = new Document(new TextNode("data"), null, "doc");
        List<Diagnostic> result = gated.validate(doc);

        assertFalse(delegate.invoked);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateShouldReturnEmptyWhenDocumentIsNull() {
        RecordingValidator delegate = new RecordingValidator("markdown");
        FormatGatedValidator gated = new FormatGatedValidator(delegate, Set.of("markdown"));

        List<Diagnostic> result = gated.validate(null);

        assertFalse(delegate.invoked);
        assertTrue(result.isEmpty());
    }

    @Test
    void delegateAccessorShouldReturnWrappedValidator() {
        RecordingValidator delegate = new RecordingValidator("markdown");
        FormatGatedValidator gated = new FormatGatedValidator(delegate, Set.of("markdown"));

        assertSame(delegate, gated.delegate());
    }

    @Test
    void supportedFormatsAccessorShouldReturnConfiguredSet() {
        FormatGatedValidator gated = new FormatGatedValidator(
                new RecordingValidator("html"),
                Set.of("html", "xml"));

        assertEquals(Set.of("html", "xml"), gated.supportedFormats());
    }

    private static class RecordingValidator implements Validator {
        private final String name;
        boolean invoked;

        RecordingValidator(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Diagnostic> validate(Document doc) {
            invoked = true;
            return List.of(new Diagnostic(Severity.WARN, "test-diag", "test", null, null));
        }
    }
}
