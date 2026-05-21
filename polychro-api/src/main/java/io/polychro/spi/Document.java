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
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * An in-memory representation of a document to be validated.
 *
 * @param root       the parsed document as a Jackson JsonNode tree;
 *                   may be {@code null} for documents whose structural projection is
 *                   deferred (e.g. raw {@code markdown} / {@code html} documents whose
 *                   parse step is skipped because no rule requires a projected tree)
 * @param format     the canonical document format identifier, may be null when unknown
 * @param sourcePath the file path or identifier of the source, may be null for in-memory documents
 * @param sourceMap  source-location resolver for projected formats
 * @param metadata   parser or projection metadata attached to the document
 */
public record Document(JsonNode root, String format, String sourcePath, SourceMap sourceMap,
                       Map<String, Object> metadata) {

    /**
     * Shared Jackson mappers. {@link ObjectMapper} (and {@link XmlMapper}) are safe for
     * concurrent read/parse operations once fully configured, so a single instance per format
     * is reused across all calls — do not introduce per-call instantiation as a "fix" for
     * apparent threading concerns.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    /**
     * XML mapper hardened against XXE / billion-laughs style attacks: the underlying
     * StAX {@link XMLInputFactory} has DTD support and external-entity resolution disabled
     * so untrusted documents cannot pull in remote resources or expand entity bombs.
     */
    private static final ObjectMapper XML_MAPPER = new XmlMapper(hardenedXmlFactory());

    private static XmlFactory hardenedXmlFactory() {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        return new XmlFactory(inputFactory);
    }

    public Document {
        String resolvedFormat = (format == null || format.isBlank())
                ? detectFormatFromSourcePath(sourcePath)
                : format;
        format = Formats.normalize(resolvedFormat);
        sourceMap = sourceMap != null ? sourceMap : SourceMap.NONE;
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public Document(JsonNode root, String sourcePath) {
        this(root, null, sourcePath, null, null);
    }

    public Document(JsonNode root, String format, String sourcePath) {
        this(root, format, sourcePath, null, null);
    }

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
            return new Document(root, "yaml", path.toString());
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
            return new Document(root, "json", path.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse JSON: " + path, e);
        }
    }

    /**
     * Parse an XML file into a Document.
     *
     * @param path the file system path to the XML document
     * @return the parsed Document
     * @throws UncheckedIOException if the file cannot be read or is not valid XML
     */
    public static Document fromXml(Path path) {
        try {
            JsonNode root = XML_MAPPER.readTree(Files.newInputStream(path));
            return new Document(root, "xml", path.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse XML: " + path, e);
        }
    }

    /**
     * Parse a string into a Document, auto-detecting format from the content when
     * {@code format} is {@code null} or blank.
     *
     * <p>Equivalent to {@link #fromString(String, String, String) fromString(content, format, null)}.
     *
     * @param content the document content as a string
     * @param format  the format: {@code "yaml"}, {@code "json"}, {@code "xml"}, {@code "markdown"},
     *                {@code "html"}, or {@code null} for auto-detection from content
     * @return the parsed Document
     * @throws UncheckedIOException     if the content is not valid for the specified format
     * @throws IllegalArgumentException if the format is not recognized
     * @see #fromString(String, String, String)
     */
    public static Document fromString(String content, String format) {
        return fromString(content, format, null);
    }

    /**
     * Parse a string into a Document, resolving the effective format from
     * {@code format} (if non-blank), then the {@code sourcePath} extension, and
     * finally from the leading bytes of the content.
     *
     * <p>The resulting {@link Document#root() root} is a structured Jackson tree
     * for {@code yaml}/{@code json}/{@code xml}, or a {@code TextNode} preserving
     * the raw content for {@code markdown}/{@code html}.
     *
     * @param content    the document content as a string (must not be {@code null} or blank)
     * @param format     the format hint: {@code "yaml"}, {@code "json"}, {@code "xml"},
     *                   {@code "markdown"}, {@code "html"}, or {@code null}/blank to
     *                   defer to {@code sourcePath} and content sniffing
     * @param sourcePath the originating path used both for format detection (via file
     *                   extension) and to populate {@link Document#sourcePath()}; may
     *                   be {@code null} when the content was not loaded from a file
     * @return the parsed Document
     * @throws IllegalArgumentException if {@code content} is {@code null} or blank,
     *                                  or if the resolved format is not recognized
     * @throws UncheckedIOException     if the content is not valid for the resolved format
     */
    public static Document fromString(String content, String format, String sourcePath) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Document content must not be null or blank");
        }
        String effectiveFormat = Formats.normalize(format);
        if (effectiveFormat == null) {
            effectiveFormat = detectFormatFromSourcePath(sourcePath);
        }
        if (effectiveFormat == null) {
            effectiveFormat = detectFormat(content);
        }
        try {
            JsonNode root = switch (effectiveFormat) {
                case "yaml" -> YAML_MAPPER.readTree(content);
                case "json" -> JSON_MAPPER.readTree(content);
                case "xml" -> XML_MAPPER.readTree(content);
                case "markdown", "html" -> TextNode.valueOf(content);
                default -> throw new IllegalArgumentException("Unknown format: " + effectiveFormat);
            };
            return new Document(root, effectiveFormat, sourcePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse content as " + effectiveFormat, e);
        }
    }

    private static String detectFormat(String content) {
        String trimmed = content.stripLeading();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return "json";
        }
        if (trimmed.startsWith("<")) {
            String lowerStart = trimmed.length() > 64
                    ? trimmed.substring(0, 64).toLowerCase(Locale.ROOT)
                    : trimmed.toLowerCase(Locale.ROOT);
            if (lowerStart.startsWith("<!doctype html") || lowerStart.startsWith("<html")) {
                return "html";
            }
            return "xml";
        }
        // Last resort: treat as YAML. Only reached when neither the source path
        // nor the leading bytes give a stronger hint, so we lean on YAML's
        // forgiving grammar as the broadest default.
        return "yaml";
    }

    private static String detectFormatFromSourcePath(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }

        String lowerSourcePath = sourcePath.toLowerCase(Locale.ROOT);
        if (lowerSourcePath.endsWith(".yml") || lowerSourcePath.endsWith(".yaml")) {
            return "yaml";
        }
        if (lowerSourcePath.endsWith(".json")) {
            return "json";
        }
        if (lowerSourcePath.endsWith(".xml")) {
            return "xml";
        }
        if (lowerSourcePath.endsWith(".md") || lowerSourcePath.endsWith(".markdown")) {
            return "markdown";
        }
        if (lowerSourcePath.endsWith(".html") || lowerSourcePath.endsWith(".htm")) {
            return "html";
        }
        return null;
    }
}
