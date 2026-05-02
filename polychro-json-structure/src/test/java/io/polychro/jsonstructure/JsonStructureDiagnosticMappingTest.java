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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import org.json_structure.validation.JsonLocation;
import org.json_structure.validation.ValidationError;
import org.json_structure.validation.ValidationSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonStructureDiagnosticMappingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void mapSeverityShouldMapErrorToError() {
        assertEquals(Severity.ERROR, JsonStructureValidator.mapSeverity(ValidationSeverity.ERROR));
    }

    @Test
    void mapSeverityShouldMapWarningToWarn() {
        assertEquals(Severity.WARN, JsonStructureValidator.mapSeverity(ValidationSeverity.WARNING));
    }

    @Test
    void mapSeverityShouldDefaultToErrorForNull() {
        assertEquals(Severity.ERROR, JsonStructureValidator.mapSeverity(null));
    }

    @Test
    void mapLocationShouldReturnNullForNullLocation() {
        assertNull(JsonStructureValidator.mapLocation(null));
    }

    @Test
    void mapLocationShouldReturnNullForUnknownLocation() {
        assertNull(JsonStructureValidator.mapLocation(JsonLocation.UNKNOWN));
    }

    @Test
    void mapLocationShouldReturnSourceRangeForKnownLocation() {
        JsonLocation loc = new JsonLocation(10, 5);
        SourceRange range = JsonStructureValidator.mapLocation(loc);
        assertNotNull(range);
        assertEquals(10, range.startLine());
        assertEquals(5, range.startColumn());
        assertEquals(10, range.endLine());
        assertEquals(5, range.endColumn());
    }

    @Test
    void toDiagnosticShouldMapAllFields() {
        ValidationError error = new ValidationError(
                "TEST_CODE", "Something went wrong", "/path/to/field",
                ValidationSeverity.ERROR, new JsonLocation(3, 7), "/schema/path");

        JsonStructureValidator validator = new JsonStructureValidator(null,
                new org.json_structure.validation.ValidationOptions(),
                JsonStructureValidator.Mode.SCHEMA);

        Diagnostic d = validator.toDiagnostic(error);

        assertEquals(Severity.ERROR, d.severity());
        assertEquals("TEST_CODE", d.code());
        assertEquals("Something went wrong", d.message());
        assertEquals("/path/to/field", d.path());
        assertNotNull(d.range());
        assertEquals(3, d.range().startLine());
        assertEquals(7, d.range().startColumn());
    }

    @Test
    void toDiagnosticShouldHandleNullLocation() {
        ValidationError error = new ValidationError(
                "CODE", "msg", "/path",
                ValidationSeverity.WARNING, null, null);

        JsonStructureValidator validator = new JsonStructureValidator(null,
                new org.json_structure.validation.ValidationOptions(),
                JsonStructureValidator.Mode.SCHEMA);

        Diagnostic d = validator.toDiagnostic(error);

        assertEquals(Severity.WARN, d.severity());
        assertNull(d.range());
    }
}
