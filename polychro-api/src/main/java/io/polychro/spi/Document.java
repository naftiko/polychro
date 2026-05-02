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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An in-memory representation of a document to be validated.
 *
 * @param root       the parsed document as a Jackson JsonNode tree
 * @param sourcePath the file path or identifier of the source, may be null for in-memory documents
 */
public record Document(JsonNode root, String sourcePath) {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Parse a YAML file into a Document.
     *
     * @param path the file system path to the YAML document
     * @return the parsed Document
     * @throws UncheckedIOException if the file cannot be read or is not valid YAML
     */
    public static Document fromYaml(Path path) {
        try {
            JsonNode root = YAML_MAPPER.readTree(Files.newInputStream(path));
            return new Document(root, path.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse YAML: " + path, e);
        }
    }

    /**
     * Parse a JSON file into a Document.
     *
     * @param path the file system path to the JSON document
     * @return the parsed Document
     * @throws UncheckedIOException if the file cannot be read or is not valid JSON
     */
    public static Document fromJson(Path path) {
        try {
            JsonNode root = JSON_MAPPER.readTree(Files.newInputStream(path));
            return new Document(root, path.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse JSON: " + path, e);
        }
    }

    /**
     * Parse a string into a Document, auto-detecting format.
     *
     * @param content the document content as a string
     * @param format  the format: "yaml", "json", or null for auto-detection
     * @return the parsed Document
     * @throws UncheckedIOException     if the content is not valid for the specified format
     * @throws IllegalArgumentException if the format is not recognized
     */
    public static Document fromString(String content, String format) {
        return fromString(content, format, null);
    }

    public static Document fromString(String content, String format, String sourcePath) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Document content must not be null or blank");
        }
        String effectiveFormat = format;
        if (effectiveFormat == null) {
            effectiveFormat = detectFormat(content);
        }
        try {
            JsonNode root = switch (effectiveFormat.toLowerCase()) {
                case "yaml", "yml" -> YAML_MAPPER.readTree(content);
                case "json" -> JSON_MAPPER.readTree(content);
                default -> throw new IllegalArgumentException("Unknown format: " + effectiveFormat);
            };
            return new Document(root, sourcePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse content as " + effectiveFormat, e);
        }
    }

    private static String detectFormat(String content) {
        String trimmed = content.stripLeading();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "json";
        }
        return "yaml";
    }
}
