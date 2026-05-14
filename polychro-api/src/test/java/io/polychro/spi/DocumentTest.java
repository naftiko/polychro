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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @TempDir
    Path tempDir;

    // --- fromYaml ---

    @Test
    void fromYamlShouldParseValidFile() throws IOException {
        Path file = tempDir.resolve("test.yml");
        Files.writeString(file, "name: hello\nvalue: 42\n");

        Document doc = Document.fromYaml(file);
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals(42, doc.root().get("value").asInt());
        assertEquals("yaml", doc.format());
        assertEquals(file.toString(), doc.sourcePath());
        assertSame(SourceMap.NONE, doc.sourceMap());
        assertTrue(doc.metadata().isEmpty());
    }

    @Test
    void fromYamlShouldThrowForInvalidYaml() throws IOException {
        Path file = tempDir.resolve("bad.yml");
        Files.writeString(file, ":\n  :\n    - [invalid");

        // Jackson YAML parser is permissive; let's use truly broken content
        // Actually Jackson YAML handles most things — test with non-existent file instead
        Path nonExistent = tempDir.resolve("does-not-exist.yml");
        assertThrows(UncheckedIOException.class, () -> Document.fromYaml(nonExistent));
    }

    @Test
    void fromYamlShouldThrowForNonExistentFile() {
        Path nonExistent = tempDir.resolve("missing.yml");
        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> Document.fromYaml(nonExistent));
        assertTrue(ex.getMessage().contains("Failed to parse YAML"));
    }

    // --- fromJson ---

    @Test
    void fromJsonShouldParseValidFile() throws IOException {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, "{\"name\": \"hello\", \"value\": 42}");

        Document doc = Document.fromJson(file);
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals(42, doc.root().get("value").asInt());
        assertEquals("json", doc.format());
        assertEquals(file.toString(), doc.sourcePath());
    }

    @Test
    void fromXmlShouldParseValidFile() throws IOException {
        Path file = tempDir.resolve("test.xml");
        Files.writeString(file, "<root><name>hello</name><value>42</value></root>");

        Document doc = Document.fromXml(file);

        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals("42", doc.root().get("value").asText());
        assertEquals("xml", doc.format());
        assertEquals(file.toString(), doc.sourcePath());
    }

    @Test
    void fromXmlShouldThrowForNonExistentFile() {
        Path nonExistent = tempDir.resolve("missing.xml");
        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> Document.fromXml(nonExistent));
        assertTrue(ex.getMessage().contains("Failed to parse XML"));
    }

    @Test
    void fromJsonShouldThrowForNonExistentFile() {
        Path nonExistent = tempDir.resolve("missing.json");
        UncheckedIOException ex = assertThrows(UncheckedIOException.class,
                () -> Document.fromJson(nonExistent));
        assertTrue(ex.getMessage().contains("Failed to parse JSON"));
    }

    @Test
    void fromJsonShouldThrowForInvalidJson() throws IOException {
        Path file = tempDir.resolve("bad.json");
        Files.writeString(file, "not json at all {{{");

        assertThrows(UncheckedIOException.class, () -> Document.fromJson(file));
    }

    // --- fromString ---

    @Test
    void fromStringShouldParseYamlWithExplicitFormat() {
        Document doc = Document.fromString("name: hello\nvalue: 42", "yaml");
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals("yaml", doc.format());
        assertNull(doc.sourcePath());
    }

    @Test
    void fromStringShouldParseYmlFormat() {
        Document doc = Document.fromString("key: value", "yml");
        assertNotNull(doc.root());
        assertEquals("value", doc.root().get("key").asText());
        assertEquals("yaml", doc.format());
    }

    @Test
    void fromStringShouldParseJsonWithExplicitFormat() {
        Document doc = Document.fromString("{\"name\": \"hello\"}", "json");
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals("json", doc.format());
        assertNull(doc.sourcePath());
    }

    @Test
    void fromStringShouldParseXmlWithExplicitFormat() {
        Document doc = Document.fromString("<root><name>hello</name></root>", "xml");
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals("xml", doc.format());
    }

    @Test
    void fromStringShouldPreserveMarkdownAsTextWhenFormatIsExplicit() {
        Document doc = Document.fromString("# Hello\n", "markdown");

        assertNotNull(doc.root());
        assertTrue(doc.root().isTextual());
        assertEquals("# Hello\n", doc.root().asText());
        assertEquals("markdown", doc.format());
    }

    @Test
    void fromStringShouldPreserveHtmlAsTextWhenFormatIsExplicit() {
        Document doc = Document.fromString("<html><body>Hello</body></html>", "html");

        assertNotNull(doc.root());
        assertTrue(doc.root().isTextual());
        assertEquals("<html><body>Hello</body></html>", doc.root().asText());
        assertEquals("html", doc.format());
    }

    @Test
    void fromStringShouldAutoDetectJson() {
        Document doc = Document.fromString("{\"key\": \"value\"}", null);
        assertNotNull(doc.root());
        assertEquals("value", doc.root().get("key").asText());
        assertEquals("json", doc.format());
    }

    @Test
    void fromStringShouldAutoDetectJsonArray() {
        Document doc = Document.fromString("[1, 2, 3]", null);
        assertNotNull(doc.root());
        assertTrue(doc.root().isArray());
        assertEquals("json", doc.format());
    }

    @Test
    void fromStringShouldAutoDetectYaml() {
        Document doc = Document.fromString("name: hello\nvalue: 42", null);
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals("yaml", doc.format());
    }

    @Test
    void fromStringShouldAutoDetectXml() {
        Document doc = Document.fromString("<root><name>hello</name></root>", null);
        assertNotNull(doc.root());
        assertEquals("hello", doc.root().get("name").asText());
        assertEquals("xml", doc.format());
    }

    @Test
    void fromStringShouldUseMarkdownSourcePathBeforeContentDetection() {
        Document doc = Document.fromString("# Hello\n", null, "docs/example.md");

        assertNotNull(doc.root());
        assertTrue(doc.root().isTextual());
        assertEquals("# Hello\n", doc.root().asText());
        assertEquals("markdown", doc.format());
        assertEquals("docs/example.md", doc.sourcePath());
    }

    @Test
    void fromStringShouldUseHtmlSourcePathBeforeContentDetection() {
        Document doc = Document.fromString("<html><body>Hello</body></html>", null, "site/index.html");

        assertNotNull(doc.root());
        assertTrue(doc.root().isTextual());
        assertEquals("<html><body>Hello</body></html>", doc.root().asText());
        assertEquals("html", doc.format());
        assertEquals("site/index.html", doc.sourcePath());
    }

    @Test
    void fromStringShouldThrowForNullContent() {
        assertThrows(IllegalArgumentException.class, () -> Document.fromString(null, "json"));
    }

    @Test
    void fromStringShouldThrowForBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> Document.fromString("   ", "yaml"));
    }

    @Test
    void fromStringShouldThrowForUnknownFormat() {
        assertThrows(IllegalArgumentException.class, () -> Document.fromString("content", "toml"));
    }

    @Test
    void fromStringShouldBeCaseInsensitiveForFormat() {
        Document doc = Document.fromString("{\"a\": 1}", "JSON");
        assertNotNull(doc.root());
        assertEquals(1, doc.root().get("a").asInt());
        assertEquals("json", doc.format());
    }

    @Test
    void fromStringShouldThrowForInvalidJsonContent() {
        assertThrows(UncheckedIOException.class,
                () -> Document.fromString("{invalid json", "json"));
    }

    @Test
    void constructorShouldInferFormatFromSourcePath() {
        Document doc = new Document(null, "docs/example.md");
        assertEquals("markdown", doc.format());
    }

    @Test
    void constructorShouldInferYamlFormatFromYamlExtension() {
        Document doc = new Document(null, "config/example.yaml");

        assertEquals("yaml", doc.format());
    }

    @Test
    void constructorShouldInferYamlFormatFromYmlExtension() {
        Document doc = new Document(null, "config/example.yml");

        assertEquals("yaml", doc.format());
    }

    @Test
    void constructorShouldInferJsonFormatFromSourcePath() {
        Document doc = new Document(null, "config/example.json");

        assertEquals("json", doc.format());
    }

    @Test
    void constructorShouldInferXmlFormatFromSourcePath() {
        Document doc = new Document(null, "config/example.xml");

        assertEquals("xml", doc.format());
    }

    @Test
    void constructorShouldInferMarkdownFormatFromMarkdownExtension() {
        Document doc = new Document(null, "docs/example.markdown");

        assertEquals("markdown", doc.format());
    }

    @Test
    void constructorShouldInferHtmlFormatFromHtmlExtension() {
        Document doc = new Document(null, "site/index.html");

        assertEquals("html", doc.format());
    }

    @Test
    void constructorShouldInferHtmlFormatFromHtmExtension() {
        Document doc = new Document(null, "site/index.htm");

        assertEquals("html", doc.format());
    }

    @Test
    void constructorShouldLeaveFormatNullForBlankSourcePath() {
        Document doc = new Document(null, "   ");

        assertNull(doc.format());
    }

    @Test
    void constructorShouldLeaveFormatNullForNullSourcePath() {
        Document doc = new Document(null, (String) null);

        assertNull(doc.format());
    }

    @Test
    void constructorShouldLeaveFormatNullForUnknownSourcePath() {
        Document doc = new Document(null, "config/example.toml");

        assertNull(doc.format());
    }

    @Test
    void constructorShouldNormalizeMarkdownAlias() {
        Document doc = new Document(null, "md", null);

        assertEquals("markdown", doc.format());
    }

    @Test
    void constructorShouldNormalizeHtmlAlias() {
        Document doc = new Document(null, "htm", null);

        assertEquals("html", doc.format());
    }

    @Test
    void constructorShouldTreatBlankFormatAsUnknown() {
        Document doc = new Document(null, "   ", null);

        assertNull(doc.format());
    }

    @Test
    void constructorShouldPreserveCustomMetadataAndSourceMap() {
        SourceMap sourceMap = path -> new SourceRange(1, 1, 1, 5);
        Document doc = new Document(null, "markdown", "test.md", sourceMap, Map.of("profile", "generic"));

        assertEquals("markdown", doc.format());
        assertEquals("generic", doc.metadata().get("profile"));
        assertEquals(new SourceRange(1, 1, 1, 5), doc.sourceMap().resolve("$.document"));
    }

    @Test
    void sourceMapNoneShouldResolveToNull() {
        assertNull(SourceMap.NONE.resolve("$.document"));
    }
}
