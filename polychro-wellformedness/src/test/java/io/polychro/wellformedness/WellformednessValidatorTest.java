package io.polychro.wellformedness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WellformednessValidatorTest {

    private WellformednessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WellformednessValidator(
                WellformednessValidator.DEFAULT_MAX_DEPTH,
                WellformednessValidator.DEFAULT_MAX_SIZE);
    }

    @Test
    void nameShouldReturnWellformedness() {
        assertEquals("wellformedness", validator.name());
    }

    @Test
    void validateShouldReturnEmptyForCleanYaml() {
        Path path = Path.of("src/test/resources/fixtures/clean.yml");
        Document doc = Document.fromYaml(path);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.isEmpty(), "Expected no diagnostics for clean YAML");
    }

    @Test
    void validateShouldReturnEmptyForCleanJson() {
        Path path = Path.of("src/test/resources/fixtures/clean.json");
        Document doc = Document.fromJson(path);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.isEmpty(), "Expected no diagnostics for clean JSON");
    }

    @Test
    void validateShouldDetectDuplicateKeysInYaml() {
        Path path = Path.of("src/test/resources/fixtures/duplicate-keys.yml");
        Document doc = Document.fromYaml(path);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "duplicate-key".equals(d.code())));
    }

    @Test
    void validateShouldDetectDuplicateKeysInJson() {
        Path path = Path.of("src/test/resources/fixtures/duplicate-keys.json");
        Document doc = Document.fromJson(path);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "duplicate-key".equals(d.code())));
    }

    @Test
    void validateShouldDetectUtf8Bom(@TempDir Path tempDir) throws IOException {
        Path bomFile = tempDir.resolve("bom.yml");
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = "name: test\n".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, withBom, 0, bom.length);
        System.arraycopy(content, 0, withBom, bom.length, content.length);
        Files.write(bomFile, withBom);

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(Files.newInputStream(bomFile));
        Document doc = new Document(root, bomFile.toString());

        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "utf8-bom".equals(d.code())));
    }

    @Test
    void validateShouldDetectInvalidEncodingBytes(@TempDir Path tempDir) throws IOException {
        Path badFile = tempDir.resolve("bad-encoding.yml");
        byte[] content = new byte[]{'n', 'a', 'm', 'e', ':', ' ', (byte) 0xFF, (byte) 0xFE, '\n'};
        Files.write(badFile, content);

        // The document may or may not parse — we create a Document with a simple root
        Document doc = new Document(JsonNodeFactory.instance.objectNode(), badFile.toString());

        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "invalid-encoding".equals(d.code())));
    }

    @Test
    void validateShouldDetectTabIndentation() {
        Path path = Path.of("src/test/resources/fixtures/tab-indentation.yml");
        // Tab indentation makes YAML unparseable, so construct Document manually
        Document doc = new Document(JsonNodeFactory.instance.objectNode(), path.toString());
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "tab-indentation".equals(d.code())));
    }

    @Test
    void validateShouldDetectNonStringYamlKeys() {
        Path path = Path.of("src/test/resources/fixtures/non-string-keys.yml");
        Document doc = Document.fromYaml(path);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "non-string-key".equals(d.code())));
    }

    @Test
    void validateShouldDetectDepthLimitExceeded() {
        WellformednessValidator shallow = new WellformednessValidator(3, WellformednessValidator.DEFAULT_MAX_SIZE);
        Path path = Path.of("src/test/resources/fixtures/deep.yml");
        Document doc = Document.fromYaml(path);
        List<Diagnostic> result = shallow.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "depth-limit-exceeded".equals(d.code())));
    }

    @Test
    void validateShouldDetectSizeLimitExceeded() {
        WellformednessValidator small = new WellformednessValidator(WellformednessValidator.DEFAULT_MAX_DEPTH, 3);
        Path path = Path.of("src/test/resources/fixtures/clean.yml");
        Document doc = Document.fromYaml(path);
        List<Diagnostic> result = small.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "size-limit-exceeded".equals(d.code())));
    }

    @Test
    void validateShouldReportEmptyDocument() {
        Document doc = new Document(NullNode.getInstance(), null);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "empty-document".equals(d.code())));
        assertEquals(Severity.WARN, result.getFirst().severity());
    }

    @Test
    void validateShouldReportEmptyDocumentForMissingNode() {
        Document doc = new Document(MissingNode.getInstance(), null);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.stream().anyMatch(d -> "empty-document".equals(d.code())));
    }

    @Test
    void validateShouldReportNullRoot() {
        Document doc = new Document(null, null);
        List<Diagnostic> result = validator.validate(doc);
        assertEquals(1, result.size());
        assertEquals("null-root", result.getFirst().code());
        assertEquals(Severity.ERROR, result.getFirst().severity());
    }

    @Test
    void validateShouldSkipRawChecksWhenSourcePathIsNull() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("name", "test");
        Document doc = new Document(root, null);
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateShouldSkipRawChecksWhenFileDoesNotExist() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("name", "test");
        Document doc = new Document(root, "/nonexistent/path/file.yml");
        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.isEmpty());
    }

    @Test
    void checkDepthShouldHandleArrays() {
        WellformednessValidator shallow = new WellformednessValidator(2, WellformednessValidator.DEFAULT_MAX_SIZE);
        // Array with nested object at depth 3
        ObjectNode inner = JsonNodeFactory.instance.objectNode();
        inner.put("key", "value");
        var array = JsonNodeFactory.instance.arrayNode();
        array.add(inner);
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.set("items", array);

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        shallow.checkDepth(root, 1, diagnostics);
        // root(1) -> items array(2) -> inner object(3) -> "value" node(4) — exceeds depth 2
        assertTrue(diagnostics.stream().anyMatch(d -> "depth-limit-exceeded".equals(d.code())));
    }

    @Test
    void countNodesShouldHandleNullNode() {
        assertEquals(0, validator.countNodes(null));
    }

    @Test
    void countNodesShouldCountObjectFields() {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("a", "1");
        obj.put("b", "2");
        // obj(1) + "1"(1) + "2"(1) = 3
        assertEquals(3, validator.countNodes(obj));
    }

    @Test
    void countNodesShouldCountArrayElements() {
        var array = JsonNodeFactory.instance.arrayNode();
        array.add("a");
        array.add("b");
        // array(1) + "a"(1) + "b"(1) = 3
        assertEquals(3, validator.countNodes(array));
    }

    @Test
    void utf8SequenceLengthShouldReturnCorrectLengths() {
        assertEquals(2, validator.utf8SequenceLength(0xC0)); // 110xxxxx
        assertEquals(3, validator.utf8SequenceLength(0xE0)); // 1110xxxx
        assertEquals(4, validator.utf8SequenceLength(0xF0)); // 11110xxx
        assertEquals(0, validator.utf8SequenceLength(0x80)); // 10xxxxxx (continuation)
    }

    @Test
    void checkEncodingShouldDetectTruncatedSequence(@TempDir Path tempDir) throws IOException {
        Path badFile = tempDir.resolve("truncated.yml");
        // Start a 3-byte sequence but only provide 2 bytes, then EOF
        byte[] content = new byte[]{'a', ':', ' ', (byte) 0xE0, (byte) 0x80};
        Files.write(badFile, content);

        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkEncoding(content, diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d ->
                "invalid-encoding".equals(d.code()) && d.message().contains("Truncated")));
    }

    @Test
    void checkEncodingShouldDetectInvalidContinuationByte() {
        // Start a 2-byte sequence (0xC0) but follow with non-continuation byte
        byte[] content = new byte[]{'a', (byte) 0xC2, 0x20};
        List<Diagnostic> diagnostics = new java.util.ArrayList<>();
        validator.checkEncoding(content, diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d ->
                "invalid-encoding".equals(d.code()) && d.message().contains("continuation")));
    }

    @Test
    void isNonStringKeyShouldReturnTrueForBooleans() {
        assertTrue(validator.isNonStringKey("true"));
        assertTrue(validator.isNonStringKey("false"));
    }

    @Test
    void isNonStringKeyShouldReturnTrueForNull() {
        assertTrue(validator.isNonStringKey("null"));
    }

    @Test
    void isNonStringKeyShouldReturnTrueForNumbers() {
        assertTrue(validator.isNonStringKey("123"));
        assertTrue(validator.isNonStringKey("3.14"));
    }

    @Test
    void isNonStringKeyShouldReturnFalseForStrings() {
        assertFalse(validator.isNonStringKey("hello"));
        assertFalse(validator.isNonStringKey("my-key"));
    }

    @Test
    void isNonStringKeyShouldReturnFalseForNull() {
        assertFalse(validator.isNonStringKey(null));
    }

    @Test
    void isYamlFileShouldDetectYmlExtension() {
        assertTrue(validator.isYamlFile(Path.of("test.yml")));
        assertTrue(validator.isYamlFile(Path.of("test.yaml")));
        assertTrue(validator.isYamlFile(Path.of("test.YML")));
    }

    @Test
    void isYamlFileShouldRejectJsonExtension() {
        assertFalse(validator.isYamlFile(Path.of("test.json")));
    }

    @Test
    void validateShouldHandleValidMultiByteUtf8(@TempDir Path tempDir) throws IOException {
        Path utf8File = tempDir.resolve("valid-utf8.yml");
        String content = "name: café\ndescription: naïve\n";
        Files.writeString(utf8File, content, StandardCharsets.UTF_8);

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode root = yamlMapper.readTree(Files.newInputStream(utf8File));
        Document doc = new Document(root, utf8File.toString());

        List<Diagnostic> result = validator.validate(doc);
        assertTrue(result.isEmpty(), "Valid UTF-8 multi-byte should not produce diagnostics");
    }

    @Test
    void checkEncodingShouldHandleShortFiles() {
        // File shorter than 3 bytes — exercises BOM length check short-circuit
        byte[] oneByteFile = new byte[]{0x61}; // 'a'
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkEncoding(oneByteFile, diagnostics);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkEncodingShouldHandleTwoBytesStartingWithBomPrefix() {
        // 2 bytes: starts with 0xEF but too short for BOM
        byte[] twoBytes = new byte[]{(byte) 0xEF, (byte) 0xBB};
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkEncoding(twoBytes, diagnostics);
        // 0xEF starts a 3-byte sequence, but only 2 bytes available → truncated
        assertTrue(diagnostics.stream().anyMatch(d -> "invalid-encoding".equals(d.code())));
    }

    @Test
    void checkEncodingShouldNotFalseBomWhenSecondByteDiffers() {
        // 3+ bytes, first byte is 0xEF but second is NOT 0xBB
        byte[] notBom = new byte[]{(byte) 0xEF, (byte) 0xBC, (byte) 0x80}; // valid 3-byte UTF-8 (U+FF00)
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkEncoding(notBom, diagnostics);
        // Should NOT report BOM
        assertTrue(diagnostics.stream().noneMatch(d -> "utf8-bom".equals(d.code())));
    }

    @Test
    void checkEncodingShouldNotFalseBomWhenThirdByteDiffers() {
        // 3 bytes, first two match BOM but third does not
        byte[] almostBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0x80};
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkEncoding(almostBom, diagnostics);
        // Should NOT report BOM (third byte is 0x80, not 0xBF)
        assertTrue(diagnostics.stream().noneMatch(d -> "utf8-bom".equals(d.code())));
        // 0xEF 0xBB 0x80 is valid UTF-8 (U+FEC0)
    }

    @Test
    void checkRawContentShouldReportReadError(@TempDir Path tempDir) {
        // Use a directory path — reading directory as file throws IOException
        Path dir = tempDir.resolve("subdir");
        dir.toFile().mkdirs();
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkRawContent(dir, diagnostics);
        assertTrue(diagnostics.stream().anyMatch(d -> "read-error".equals(d.code())));
    }

    @Test
    void checkYamlKeysShouldHandleParseErrors() {
        // Tab indentation causes YAMLParser to throw during parsing
        Path tabFile = Path.of("src/test/resources/fixtures/tab-indentation.yml");
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkYamlKeys(tabFile, diagnostics);
        // Exception from parse error is swallowed — no diagnostic added for parse errors
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkYamlKeysShouldHandleFileNotFound() {
        // Non-existent file causes IOException from Files.newInputStream
        Path noFile = Path.of("nonexistent-file.yml");
        List<Diagnostic> diagnostics = new ArrayList<>();
        validator.checkYamlKeys(noFile, diagnostics);
        assertTrue(diagnostics.isEmpty());
    }
}
