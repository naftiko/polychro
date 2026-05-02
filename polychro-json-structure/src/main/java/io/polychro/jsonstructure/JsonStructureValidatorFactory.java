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
package io.polychro.jsonstructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;
import org.json_structure.validation.ValidationOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Factory for creating {@link JsonStructureValidator} instances.
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>{@code schemaNode} — inline {@link JsonNode} representing the schema</li>
 *   <li>{@code schemaPath} — path to a JSON Structure schema file (filesystem or classpath)</li>
 *   <li>{@code mode} — "schema" or "instance" (default: "instance" when schema is provided, "schema" otherwise)</li>
 *   <li>{@code externalSchemas} — {@code Map<String, JsonNode>} for offline $import resolution</li>
 *   <li>{@code strictFormatValidation} — Boolean, enables strict format validation</li>
 * </ul>
 */
public class JsonStructureValidatorFactory implements ValidatorFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return JsonStructureValidator.NAME;
    }

    @Override
    public Validator create(ValidatorConfig config) {
        JsonNode schemaNode = resolveSchemaNode(config);
        ValidationOptions options = buildOptions(config);
        JsonStructureValidator.Mode mode = resolveMode(config, schemaNode);
        return new JsonStructureValidator(schemaNode, options, mode);
    }

    @SuppressWarnings("unchecked")
    ValidationOptions buildOptions(ValidatorConfig config) {
        ValidationOptions options = new ValidationOptions();

        config.get("strictFormatValidation", Boolean.class)
                .ifPresent(options::setStrictFormatValidation);

        config.get("externalSchemas", Map.class)
                .ifPresent(schemas -> options.setExternalSchemas((Map<String, JsonNode>) schemas));

        config.get("allowImport", Boolean.class)
                .ifPresent(options::setAllowImport);

        return options;
    }

    JsonStructureValidator.Mode resolveMode(ValidatorConfig config, JsonNode schemaNode) {
        String modeStr = config.get("mode", String.class).orElse(null);
        if (modeStr != null) {
            return switch (modeStr.toLowerCase()) {
                case "schema" -> JsonStructureValidator.Mode.SCHEMA;
                default -> JsonStructureValidator.Mode.INSTANCE;
            };
        }
        // Auto-detect: if no schema provided, validate as schema document
        if (schemaNode == null) {
            return JsonStructureValidator.Mode.SCHEMA;
        }
        return JsonStructureValidator.Mode.INSTANCE;
    }

    JsonNode resolveSchemaNode(ValidatorConfig config) {
        // Prefer inline schemaNode
        JsonNode inline = config.get("schemaNode", JsonNode.class).orElse(null);
        if (inline != null) {
            return inline;
        }

        // Try schemaPath (filesystem first, then classpath)
        String schemaPath = config.get("schemaPath", String.class).orElse(null);
        if (schemaPath == null) {
            return null; // Schema mode — validate the document itself as a schema
        }

        // Filesystem path
        Path fsPath = Path.of(schemaPath);
        if (Files.exists(fsPath)) {
            try {
                return MAPPER.readTree(Files.readString(fsPath));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read schema from path: " + schemaPath, e);
            }
        }

        // Classpath resource
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Schema not found on filesystem or classpath: " + schemaPath);
            }
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read schema from classpath: " + schemaPath, e);
        }
    }
}
