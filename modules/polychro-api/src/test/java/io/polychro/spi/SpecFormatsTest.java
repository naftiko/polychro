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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecFormatsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void detectShouldReturnOas2ForSwaggerTwo() throws Exception {
        JsonNode root = JSON.readTree("{\"swagger\": \"2.0\", \"info\": {}, \"paths\": {}}");
        assertEquals(List.of("oas2"), SpecFormats.detect(root));
    }

    @Test
    void detectShouldReturnOas3ForOpenapiThree() throws Exception {
        JsonNode root = JSON.readTree("{\"openapi\": \"3.0.2\", \"info\": {}, \"paths\": {}}");
        assertEquals(List.of("oas3"), SpecFormats.detect(root));
    }

    @Test
    void detectShouldReturnOas3ForOpenapiThreeOne() throws Exception {
        JsonNode root = JSON.readTree("{\"openapi\": \"3.1.0\"}");
        assertEquals(List.of("oas3"), SpecFormats.detect(root));
    }

    @Test
    void detectShouldReturnAas2ForAsyncapiTwo() throws Exception {
        JsonNode root = JSON.readTree("{\"asyncapi\": \"2.6.0\"}");
        assertEquals(List.of("aas2"), SpecFormats.detect(root));
    }

    @Test
    void detectShouldReturnAas3ForAsyncapiThree() throws Exception {
        JsonNode root = JSON.readTree("{\"asyncapi\": \"3.0.0\"}");
        assertEquals(List.of("aas3"), SpecFormats.detect(root));
    }

    @Test
    void detectShouldReturnEmptyForUnrecognizedAsyncapiMajorVersion() throws Exception {
        JsonNode root = JSON.readTree("{\"asyncapi\": \"1.2.0\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForPlainYamlDocument() throws Exception {
        JsonNode root = JSON.readTree("{\"name\": \"hello\", \"value\": 42}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForNullRoot() {
        assertTrue(SpecFormats.detect(null).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForNonObjectRoot() throws Exception {
        JsonNode root = JSON.readTree("[1, 2, 3]");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForSwaggerOne() throws Exception {
        JsonNode root = JSON.readTree("{\"swagger\": \"1.2\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForNonNumericVersionText() throws Exception {
        JsonNode root = JSON.readTree("{\"swagger\": \"not-a-version\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldHandleNumericVersionNode() {
        JsonNode root = JsonNodeFactory.instance.objectNode()
                .put("openapi", 3);
        assertEquals(List.of("oas3"), SpecFormats.detect(root));
    }

    @Test
    void detectShouldAccumulateBothFormatsWhenSwaggerAndOpenapiBothPresent() throws Exception {
        // Not a realistic document, but guards that format detection is independent per
        // Spectral's own predicates: swagger:2 and openapi:3 both match when both keys are
        // present, rather than short-circuiting on the first match found.
        JsonNode root = JSON.readTree("{\"swagger\": \"2.0\", \"openapi\": \"3.0.0\"}");
        assertEquals(List.of("oas2", "oas3"), SpecFormats.detect(root));
    }

    @Test
    void isSpecFormatShouldRecognizeAllFourIdentifiers() {
        assertTrue(SpecFormats.isSpecFormat("oas2"));
        assertTrue(SpecFormats.isSpecFormat("oas3"));
        assertTrue(SpecFormats.isSpecFormat("aas2"));
        assertTrue(SpecFormats.isSpecFormat("aas3"));
        assertTrue(SpecFormats.isSpecFormat("OAS3"));
    }

    @Test
    void isSpecFormatShouldRejectSyntaxFormatsAndNull() {
        assertFalse(SpecFormats.isSpecFormat("yaml"));
        assertFalse(SpecFormats.isSpecFormat("json"));
        assertFalse(SpecFormats.isSpecFormat(null));
    }

    @Test
    void detectShouldReturnEmptyForOpenapiMajorVersionTwo() throws Exception {
        // openapi major version other than 3 (e.g. an incorrectly declared "openapi: 2.0") is not
        // a recognized oas3 document — exercises the openapiMajor != null but != 3 combination.
        JsonNode root = JSON.readTree("{\"openapi\": \"2.0\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForOverflowingVersionNumber() throws Exception {
        // A leading digit run too large to fit an int must be treated as unrecognized rather than
        // throwing — exercises the NumberFormatException catch branch in majorVersion.
        JsonNode root = JSON.readTree("{\"swagger\": \"99999999999999.0\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForExplicitJsonNullVersionValue() throws Exception {
        // A present key with an explicit JSON null value (versionNode != null but
        // versionNode.isNull() == true) is distinct from an absent key (versionNode == null) —
        // exercises the isNull() branch of majorVersion's guard independently of the null-node
        // branch.
        JsonNode root = JSON.readTree("{\"openapi\": null}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    // --- asyncapi requires a full major.minor.patch semver, unlike oas2/oas3 ---

    @Test
    void detectShouldReturnEmptyForAsyncapiMajorVersionOnly() throws Exception {
        // Spectral's aas2Regex/aas3Regex require a complete major.minor.patch string; a bare
        // major version like "2" is NOT a recognized aas2 document even though majorVersion()
        // (used for the lenient oas2/oas3 check) would happily parse its leading digit.
        JsonNode root = JSON.readTree("{\"asyncapi\": \"2\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForAsyncapiMajorMinorOnly() throws Exception {
        // "2.6" is missing the patch component the strict aas2Regex requires.
        JsonNode root = JSON.readTree("{\"asyncapi\": \"2.6\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForAsyncapiTrailingGarbageAfterVersion() throws Exception {
        // "2junk" starts with a recognizable major-version digit but is not a valid semver —
        // must stay unrecognized, matching Spectral rather than the lenient leading-digits parse.
        JsonNode root = JSON.readTree("{\"asyncapi\": \"2junk\"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForExplicitJsonNullAsyncapiValue() throws Exception {
        JsonNode root = JSON.readTree("{\"asyncapi\": null}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }

    @Test
    void detectShouldReturnEmptyForAsyncapiVersionPaddedWithWhitespace() throws Exception {
        // Spectral applies its anchored aas2Regex/aas3Regex directly to String(asyncapi)
        // WITHOUT trimming, so a padded value like " 2.6.0 " does not match either regex.
        // versionText() must mirror that — stripping here would incorrectly classify this as aas2.
        JsonNode root = JSON.readTree("{\"asyncapi\": \" 2.6.0 \"}");
        assertTrue(SpecFormats.detect(root).isEmpty());
    }
}
