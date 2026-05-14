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
package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFormatTest {

    private final MarkdownFormat format = new MarkdownFormat() {
        @Override
        public void validate(Document doc, java.util.List<io.polychro.spi.Diagnostic> diagnostics) {
            // No-op stub for default helper coverage.
        }
    };

    @Test
    void frontmatterShouldReturnNullWhenDocumentIsNull() {
        assertNull(format.frontmatter(null));
    }

    @Test
    void frontmatterShouldReturnNullWhenRootIsMissing() {
        assertNull(format.frontmatter(new Document(null, "markdown", null)));
    }

    @Test
    void frontmatterShouldReturnNullWhenProjectedFrontmatterIsNull() {
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putNull("frontmatter");

        assertNull(format.frontmatter(new Document(root, "markdown", null)));
    }

    @Test
    void frontmatterShouldReturnNullWhenProjectedFrontmatterIsMissing() {
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document");

        assertNull(format.frontmatter(new Document(root, "markdown", null)));
    }

    @Test
    void frontmatterShouldReturnProjectedNodeWhenPresent() {
        var frontmatter = JsonNodeFactory.instance.objectNode();
        frontmatter.put("name", "demo");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").set("frontmatter", frontmatter);

        assertEquals("demo", format.frontmatter(new Document(root, "markdown", null)).path("name").asText());
    }

    @Test
    void headingsShouldReturnEmptyWhenRootIsMissing() {
        int count = 0;
        for (var ignored : format.headings(new Document(null, "markdown", null))) {
            count++;
        }

        assertEquals(0, count);
    }

    @Test
    void headingsShouldReturnEmptyWhenDocumentIsNull() {
        int count = 0;
        for (var ignored : format.headings(null)) {
            count++;
        }

        assertEquals(0, count);
    }

    @Test
    void headingsShouldReturnEmptyWhenProjectedHeadingsAreNotAnArray() {
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putObject("headings");

        int count = 0;
        for (var ignored : format.headings(new Document(root, "markdown", null))) {
            count++;
        }

        assertEquals(0, count);
    }

    @Test
    void headingsShouldReturnEmptyWhenProjectedHeadingsAreMissing() {
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document");

        int count = 0;
        for (var ignored : format.headings(new Document(root, "markdown", null))) {
            count++;
        }

        assertEquals(0, count);
    }

    @Test
    void headingsShouldReturnProjectedHeadingsWhenArrayIsPresent() {
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 2);
        heading.put("text", "Overview");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        int count = 0;
        for (var projectedHeading : format.headings(new Document(root, "markdown", null))) {
            assertEquals("Overview", projectedHeading.path("text").asText());
            count++;
        }

        assertEquals(1, count);
    }

    @Test
    void headingsShouldPreferBlocksWhenPresent() {
        var blockHeading = JsonNodeFactory.instance.objectNode();
        blockHeading.put("type", "heading");
        blockHeading.put("level", 2);
        blockHeading.put("text", "Overview");
        var legacyHeading = JsonNodeFactory.instance.objectNode();
        legacyHeading.put("level", 2);
        legacyHeading.put("text", "Legacy");
        var root = JsonNodeFactory.instance.objectNode();
        var document = root.putObject("document");
        document.putArray("blocks").add(blockHeading);
        document.putArray("headings").add(legacyHeading);

        int count = 0;
        for (var projectedHeading : format.headings(new Document(root, "markdown", null))) {
            assertEquals("Overview", projectedHeading.path("text").asText());
            count++;
        }

        assertEquals(1, count);
    }

    @Test
    void hasHeadingShouldReturnTrueForMatchingHeadingAndLevel() {
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 2);
        heading.put("text", "Overview");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        assertTrue(format.hasHeading(new Document(root, "markdown", null), "Overview", 2));
    }

    @Test
    void hasHeadingShouldReturnFalseWhenHeadingLevelIsTooLow() {
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 1);
        heading.put("text", "Overview");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        assertFalse(format.hasHeading(new Document(root, "markdown", null), "Overview", 2));
    }

    @Test
    void hasHeadingShouldReturnFalseWhenHeadingTextDoesNotMatch() {
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 2);
        heading.put("text", "Overview");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        assertFalse(format.hasHeading(new Document(root, "markdown", null), "Other", 2));
    }

    @Test
    void hasHeadingAtOrAboveLevelShouldReturnFalseWhenNoHeadingMatches() {
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 1);
        heading.put("text", "Intro");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        assertFalse(format.hasHeadingAtOrAboveLevel(new Document(root, "markdown", null), 2));
    }

    @Test
    void hasHeadingAtOrAboveLevelShouldReturnTrueWhenHeadingMatches() {
        var heading = JsonNodeFactory.instance.objectNode();
        heading.put("level", 3);
        heading.put("text", "Details");
        var root = JsonNodeFactory.instance.objectNode();
        root.putObject("document").putArray("headings").add(heading);

        assertTrue(format.hasHeadingAtOrAboveLevel(new Document(root, "markdown", null), 2));
    }
}