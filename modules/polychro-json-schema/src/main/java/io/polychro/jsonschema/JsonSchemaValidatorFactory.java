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
package io.polychro.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SpecVersion;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Factory for creating {@link JsonSchemaValidator} instances.
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>{@code schemaPath} — path to a JSON Schema file (filesystem or classpath)</li>
 *   <li>{@code schemaNode} — inline {@link JsonNode} representing the schema</li>
 * </ul>
 */
public class JsonSchemaValidatorFactory implements ValidatorFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return JsonSchemaValidator.NAME;
    }

    @Override
    public Validator create(ValidatorConfig config) {
        JsonNode schemaNode = resolveSchemaNode(config);
        SpecVersion.VersionFlag version = JsonSchemaValidator.detectDraft(schemaNode);
        JsonSchema schema = JsonSchemaValidator.buildSchema(schemaNode, version);
        return new JsonSchemaValidator(schema);
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
            throw new IllegalArgumentException(
                    "JsonSchemaValidatorFactory requires either 'schemaNode' or 'schemaPath' in config");
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
