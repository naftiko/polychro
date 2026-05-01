package io.polychro.spike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 0 — JSONPath Compatibility Spike.
 *
 * Validates that Jayway JsonPath can evaluate the JSONPath expressions used in
 * Spectral-format rulesets (Naftiko rules + spectral:oas patterns).
 *
 * Classification of results:
 *   (a) syntax not supported — expression throws or returns unexpected shape
 *   (b) supported but different semantics — parses but different node set
 *   (c) identical — works as expected
 */
class JsonPathCompatibilityTest {

    static Configuration JSONPATH_CONFIG;
    static Object naftikoDoc;
    static Object oasDoc;

    @BeforeAll
    static void setUp() throws IOException {
        JSONPATH_CONFIG = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
                .build();

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectMapper jsonMapper = new ObjectMapper();

        try (InputStream yamlStream = JsonPathCompatibilityTest.class.getResourceAsStream("/fixtures/naftiko-capability.yml");
             InputStream jsonStream = JsonPathCompatibilityTest.class.getResourceAsStream("/fixtures/openapi-3.1.json")) {
            JsonNode yamlNode = yamlMapper.readTree(yamlStream);
            naftikoDoc = JSONPATH_CONFIG.jsonProvider().parse(jsonMapper.writeValueAsString(yamlNode));
            oasDoc = JSONPATH_CONFIG.jsonProvider().parse(jsonStream, "UTF-8");
        }
    }

    /** Evaluate a JSONPath expression and return result as ArrayNode. */
    static ArrayNode eval(Object doc, String path) {
        return JsonPath.using(JSONPATH_CONFIG).parse(doc).read(path);
    }

    static void assertNonEmpty(Object doc, String path, String msg) {
        ArrayNode result = eval(doc, path);
        assertNotNull(result, "Result should not be null for: " + path);
        assertFalse(result.isEmpty(), msg + " [" + path + "]");
    }

    static void assertParseable(Object doc, String path) {
        assertDoesNotThrow(() -> {
            ArrayNode result = eval(doc, path);
            assertNotNull(result);
        }, "Expression should be parseable: " + path);
    }

    // ─────────────────────────────────────────────────────────────────
    // NAFTIKO RULESET EXPRESSIONS
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Naftiko Rules — Simple Paths")
    class NaftikoSimplePaths {

        @Test
        @DisplayName("$ (root document)")
        void rootDocument() {
            assertNonEmpty(naftikoDoc, "$", "Root should return the document");
        }

        @Test
        @DisplayName("$.info")
        void infoObject() {
            assertNonEmpty(naftikoDoc, "$.info", "Should find info object");
        }

        @Test
        @DisplayName("$.info.description")
        void infoDescription() {
            assertNonEmpty(naftikoDoc, "$.info.description", "Should find info.description");
        }

        @Test
        @DisplayName("$.info.label")
        void infoLabel() {
            assertNonEmpty(naftikoDoc, "$.info.label", "Should find info.label");
        }

        @Test
        @DisplayName("$.capability.consumes[*].baseUri")
        void capabilityConsumesBaseUri() {
            assertNonEmpty(naftikoDoc, "$.capability.consumes[*].baseUri", "Should find baseUri in capability.consumes");
        }

        @Test
        @DisplayName("$.capability.aggregates[*].functions[*]")
        void aggregateFunctions() {
            assertNonEmpty(naftikoDoc, "$.capability.aggregates[*].functions[*]", "Should find aggregate functions");
        }

        @Test
        @DisplayName("$.consumes[*]")
        void topLevelConsumes() {
            // Top-level consumes doesn't exist in our fixture — should parse without error
            assertParseable(naftikoDoc, "$.consumes[*]");
        }
    }

    @Nested
    @DisplayName("Naftiko Rules — Filter Expressions")
    class NaftikoFilterExpressions {

        @Test
        @DisplayName("$.capability.consumes[?(@.type == 'http')].resources[*].path")
        void capabilityConsumesHttpResourcePath() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.consumes[?(@.type == 'http')].resources[*].path",
                    "Should find paths in HTTP consumes resources");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'rest')].resources[*].path")
        void exposesRestResourcePath() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'rest')].resources[*].path",
                    "Should find REST resource paths");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*]")
        void exposesRestOperations() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*]",
                    "Should find REST operations");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*].description")
        void exposesRestOperationDescription() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*].description",
                    "Should find REST operation descriptions");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*].steps[*].name")
        void exposesRestStepsName() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*].steps[*].name",
                    "Should find step names");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'mcp')].tools[*].description")
        void exposesMcpToolDescription() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'mcp')].tools[*].description",
                    "Should find MCP tool descriptions");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'mcp')].address")
        void exposesMcpAddress() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'mcp')].address",
                    "Should find MCP address");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'rest')].address")
        void exposesRestAddress() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'rest')].address",
                    "Should find REST address");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'skill')].address")
        void exposesSkillAddress() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'skill')].address",
                    "Should find skill address");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'control')].address")
        void exposesControlAddress() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'control')].address",
                    "Should find control address");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'mcp' && @.transport == 'stdio')] — compound filter, no match")
        void exposesMcpStdioCompoundFilter() {
            ArrayNode result = eval(naftikoDoc, "$.capability.exposes[?(@.type == 'mcp' && @.transport == 'stdio')]");
            assertNotNull(result);
            assertTrue(result.isEmpty(), "No stdio MCP in fixture");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'mcp' && @.transport == 'http')] — compound filter, match")
        void exposesMcpHttpCompoundFilter() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'mcp' && @.transport == 'http')]",
                    "Should find MCP with http transport");
        }

        @Test
        @DisplayName("$.capability.exposes[*].authentication[?(@.type == 'oauth2')]")
        void exposesOauth2Authentication() {
            // No oauth2 in fixture — should parse without error, return empty
            assertParseable(naftikoDoc, "$.capability.exposes[*].authentication[?(@.type == 'oauth2')]");
        }

        @Test
        @DisplayName("$.capability.exposes[*].description")
        void exposesDescription() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[*].description",
                    "Should find exposes descriptions");
        }

        @Test
        @DisplayName("$.capability.consumes[*].description")
        void capabilityConsumesDescription() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.consumes[*].description",
                    "Should find consumes descriptions");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'skill')].skills[*].description")
        void exposesSkillDescription() {
            assertNonEmpty(naftikoDoc,
                    "$.capability.exposes[?(@.type == 'skill')].skills[*].description",
                    "Should find skill descriptions");
        }

        @Test
        @DisplayName("$.capability.exposes[?(@.type == 'mcp')].tools[*].steps[*].name")
        void exposesMcpToolStepsName() {
            // MCP tools in fixture use `call` not `steps` — empty is OK, must parse
            assertParseable(naftikoDoc, "$.capability.exposes[?(@.type == 'mcp')].tools[*].steps[*].name");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // OPENAPI (spectral:oas) EXPRESSIONS
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("OAS Rules — Simple Paths")
    class OasSimplePaths {

        @Test
        void infoContact() {
            assertNonEmpty(oasDoc, "$.info.contact", "Should find info.contact");
        }

        @Test
        void infoDescription() {
            assertNonEmpty(oasDoc, "$.info.description", "Should find info.description");
        }

        @Test
        void infoLicense() {
            assertNonEmpty(oasDoc, "$.info.license", "Should find info.license");
        }

        @Test
        void infoLicenseUrl() {
            assertNonEmpty(oasDoc, "$.info.license.url", "Should find info.license.url");
        }

        @Test
        void paths() {
            assertNonEmpty(oasDoc, "$.paths", "Should find paths");
        }

        @Test
        void tags() {
            ArrayNode result = eval(oasDoc, "$.tags[*]");
            assertEquals(2, result.size(), "Should find 2 tags");
        }

        @Test
        void serversUrl() {
            assertNonEmpty(oasDoc, "$.servers[*].url", "Should find server URLs");
        }

        @Test
        void componentsExamples() {
            assertNonEmpty(oasDoc, "$.components.examples[*]", "Should find examples");
        }

        @Test
        void componentsParameters() {
            assertNonEmpty(oasDoc, "$.components.parameters[*]", "Should find parameters");
        }

        @Test
        void componentsResponses() {
            assertNonEmpty(oasDoc, "$.components.responses[*]", "Should find responses");
        }

        @Test
        void componentsLinks() {
            assertNonEmpty(oasDoc, "$.components.links[*]", "Should find links");
        }

        @Test
        void securityRequirement() {
            assertNonEmpty(oasDoc, "$.security[*]", "Should find security");
        }
    }

    @Nested
    @DisplayName("OAS Rules — PathItem and OperationObject Patterns")
    class OasPathItemPatterns {

        @Test
        @DisplayName("$.paths[*] (PathItem alias)")
        void pathItems() {
            assertNonEmpty(oasDoc, "$.paths[*]", "Should find path items");
        }

        @ParameterizedTest
        @ValueSource(strings = {"get", "post", "delete"})
        @DisplayName("$.paths[*].{method}")
        void operationsByMethod(String method) {
            assertNonEmpty(oasDoc, "$.paths[*]." + method, "Should find " + method + " operations");
        }

        @Test
        @DisplayName("$.paths[*]['get','post','delete'].description — union notation")
        void operationDescriptions() {
            assertNonEmpty(oasDoc,
                    "$.paths[*]['get','post','delete'].description",
                    "Should find operation descriptions via union");
        }

        @Test
        @DisplayName("$.paths[*]['get','put','post','delete','options','head','patch','trace'].operationId")
        void allOperationIds() {
            assertNonEmpty(oasDoc,
                    "$.paths[*]['get','put','post','delete','options','head','patch','trace'].operationId",
                    "Should find operationIds");
        }

        @Test
        @DisplayName("$.paths[*]['get','post','delete'].parameters[*]")
        void operationParameters() {
            assertNonEmpty(oasDoc,
                    "$.paths[*]['get','post','delete'].parameters[*]",
                    "Should find operation parameters");
        }

        @Test
        @DisplayName("$.paths[*]['get','post','delete'].responses[*]")
        void operationResponses() {
            assertNonEmpty(oasDoc,
                    "$.paths[*]['get','post','delete'].responses[*]",
                    "Should find operation responses");
        }

        @Test
        @DisplayName("$.paths[*]['get','post','delete'].security[*]")
        void operationSecurity() {
            assertNonEmpty(oasDoc,
                    "$.paths[*]['get','post','delete'].security[*]",
                    "Should find operation security");
        }

        @Test
        @DisplayName("$.paths[*]['get','post','delete'].tags")
        void operationTags() {
            assertNonEmpty(oasDoc,
                    "$.paths[*]['get','post','delete'].tags",
                    "Should find operation tags");
        }
    }

    @Nested
    @DisplayName("OAS Rules — Recursive Descent")
    class OasRecursiveDescent {

        @Test
        @DisplayName("$..[description,title] — DIVERGENCE: bare identifiers not supported in recursive bracket notation")
        void recursiveDescriptionTitle() {
            // Jayway does NOT support $..[description,title] (bare identifiers in recursive descent).
            // Workaround: use $..['description','title'] (quoted keys) — but Jayway doesn't support that either.
            // Alternative: query $..*  and post-filter, or use two separate queries.
            assertThrows(Exception.class, () -> {
                Configuration strict = Configuration.builder()
                        .jsonProvider(new JacksonJsonNodeJsonProvider())
                        .mappingProvider(new JacksonMappingProvider())
                        .options(Option.ALWAYS_RETURN_LIST)
                        .build();
                JsonPath.using(strict).parse(oasDoc).read("$..[description,title]");
            }, "[DIVERGENCE-a] $..[description,title] not supported — bare identifiers in recursive bracket notation");
        }

        @Test
        @DisplayName("$..anyOf — recursive key")
        void recursiveAnyOf() {
            assertParseable(oasDoc, "$..anyOf");
        }

        @Test
        @DisplayName("$..oneOf — recursive key")
        void recursiveOneOf() {
            assertParseable(oasDoc, "$..oneOf");
        }

        @Test
        @DisplayName("$..parameters[*] — recursive parameters")
        void recursiveParameters() {
            assertNonEmpty(oasDoc, "$..parameters[*]", "Should find parameters recursively");
        }
    }

    @Nested
    @DisplayName("OAS Rules — Filter Expressions")
    class OasFilterExpressions {

        @Test
        @DisplayName("$..parameters[?(@ && @.in)] — filter on property existence")
        void parametersWithIn() {
            assertNonEmpty(oasDoc, "$..parameters[?(@ && @.in)]", "Should find parameters with 'in' property");
        }

        @Test
        @DisplayName("$.components.parameters[?(@ && @.in)] — DIVERGENCE: named object map vs array filter")
        void componentParametersWithIn() {
            // Jayway treats $.components.parameters as a map (object with named keys).
            // Filter [?(@ && @.in)] iterates map values but returns empty because
            // Jayway's object-property iteration with filters behaves differently than
            // Spectral's Nimma engine which flattens named entries for filtering.
            ArrayNode result = eval(oasDoc, "$.components.parameters[?(@ && @.in)]");
            assertNotNull(result);
            // Document: this returns empty in Jayway but non-empty in Spectral
            System.out.println("[DIVERGENCE-b] $.components.parameters[?(@ && @.in)]: " + result.size() + " results");
        }

        @Test
        @DisplayName("$..[?(@ && @.enum && @.type)] — typed-enum filter")
        void typedEnumFilter() {
            assertNonEmpty(oasDoc, "$..[?(@ && @.enum && @.type)]", "Should find objects with both enum and type");
        }

        @Test
        @DisplayName("$..[?(@ && @.type=='array')] — type filter")
        void arrayTypeFilter() {
            assertNonEmpty(oasDoc, "$..[?(@ && @.type=='array')]", "Should find array-typed schemas");
        }

        @Test
        @DisplayName("$.paths[*]['get','post','delete'].responses[*].links[*]")
        void responseLinks() {
            assertParseable(oasDoc, "$.paths[*]['get','post','delete'].responses[*].links[*]");
        }

        @Test
        @DisplayName("$..headers..[?(@ && @.schema)]")
        void headersWithSchema() {
            assertParseable(oasDoc, "$..headers..[?(@ && @.schema)]");
        }

        @Test
        @DisplayName("$.components.schemas..[?(@ && @.enum)]")
        void schemasWithEnum() {
            assertNonEmpty(oasDoc, "$.components.schemas..[?(@ && @.enum)]", "Should find schemas with enum");
        }
    }

    @Nested
    @DisplayName("OAS Rules — Known Divergences")
    class OasKnownDivergences {

        @Test
        @DisplayName("[DIVERGENCE-a] @property filter — not supported by Jayway")
        void propertyFilterNotSupported() {
            // @property is a Spectral/Nimma extension not available in Jayway JsonPath.
            // Jayway throws InvalidPathException.
            assertThrows(Exception.class, () -> {
                Configuration strict = Configuration.builder()
                        .jsonProvider(new JacksonJsonNodeJsonProvider())
                        .mappingProvider(new JacksonMappingProvider())
                        .options(Option.ALWAYS_RETURN_LIST)
                        .build();
                JsonPath.using(strict).parse(oasDoc).read("$..[?(@property !== 'properties' && @ && @.enum)]");
            }, "@property is not supported");
        }

        @Test
        @DisplayName("[DIVERGENCE-a] @property === '$ref' — not supported by Jayway")
        void refSiblingsNotSupported() {
            assertThrows(Exception.class, () -> {
                Configuration strict = Configuration.builder()
                        .jsonProvider(new JacksonJsonNodeJsonProvider())
                        .mappingProvider(new JacksonMappingProvider())
                        .options(Option.ALWAYS_RETURN_LIST)
                        .build();
                JsonPath.using(strict).parse(oasDoc).read("$..[?(@property === '$ref')]");
            }, "@property is not supported");
        }

        @Test
        @DisplayName("[DIVERGENCE-a] void 0 in filter — JavaScript syntax not supported")
        void voidZeroNotSupported() {
            // 'void 0' is JavaScript for undefined — not valid JSONPath
            assertThrows(Exception.class, () -> {
                Configuration strict = Configuration.builder()
                        .jsonProvider(new JacksonJsonNodeJsonProvider())
                        .mappingProvider(new JacksonMappingProvider())
                        .options(Option.ALWAYS_RETURN_LIST)
                        .build();
                JsonPath.using(strict).parse(oasDoc)
                        .read("$..content..[?(@ && @.schema && (@.example !== void 0 || @.examples))]");
            }, "void 0 is JavaScript syntax, not JSONPath");
        }
    }

    @Nested
    @DisplayName("OAS Rules — Edge Cases")
    class OasEdgeCases {

        @Test
        void definitionsDiscriminator() {
            assertParseable(oasDoc, "$.definitions[?(@.discriminator)]");
        }

        @Test
        void pathLevelServers() {
            assertParseable(oasDoc, "$.paths[*].servers[*]");
        }

        @Test
        void operationLevelServers() {
            assertParseable(oasDoc, "$.paths[*]['get','post','delete'].servers[*]");
        }

        @Test
        void callbacksInCallbacks() {
            assertParseable(oasDoc, "$.paths[*]['get','post','delete'].callbacks[*][*][*].callbacks");
        }

        @Test
        void webhooksServers() {
            assertParseable(oasDoc, "$.webhooks.servers");
        }

        @Test
        void webhooksOperationServers() {
            assertParseable(oasDoc, "$.webhooks[*][*].servers");
        }

        @Test
        void webhooksCallbacks() {
            assertParseable(oasDoc, "$.webhooks[*][*].callbacks");
        }

        @Test
        void serverObjects() {
            assertNonEmpty(oasDoc, "$.servers[*]", "Should find server objects");
        }

        @Test
        void contentExamples() {
            assertParseable(oasDoc, "$.paths[*]..content[*].examples[*]");
        }

        @Test
        void parameterExamples() {
            assertParseable(oasDoc, "$.paths[*]..parameters[*].examples[*]");
        }

        @Test
        void headerExamples() {
            assertParseable(oasDoc, "$.components.headers[*].examples[*]");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // CROSS-CUTTING: All expressions must parse without error
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cross-cutting — All Naftiko expressions parse successfully")
    class NaftikoExpressionValidity {

        @ParameterizedTest
        @ValueSource(strings = {
                "$",
                "$.info",
                "$.info.description",
                "$.info.label",
                "$.consumes[*].baseUri",
                "$.capability.consumes[*].baseUri",
                "$.consumes[?(@.type == 'http')].resources[*].path",
                "$.capability.consumes[?(@.type == 'http')].resources[*].path",
                "$.capability.exposes[?(@.type == 'rest')].resources[*].path",
                "$.capability.exposes[?(@.type == 'rest')].address",
                "$.capability.exposes[?(@.type == 'mcp')].address",
                "$.capability.exposes[?(@.type == 'skill')].address",
                "$.capability.aggregates[*].functions[*]",
                "$.capability.exposes[?(@.type == 'mcp' && @.transport == 'stdio')]",
                "$.capability.exposes[*].authentication[?(@.type == 'oauth2')]",
                "$.capability.exposes[*].description",
                "$.capability.exposes[?(@.type == 'rest')].resources[*].description",
                "$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*].description",
                "$.capability.exposes[?(@.type == 'mcp')].tools[*].description",
                "$.capability.exposes[?(@.type == 'skill')].skills[*].description",
                "$.consumes[*].description",
                "$.capability.consumes[*].description",
                "$.capability.exposes[?(@.type == 'rest')].resources[*].operations[*].steps[*].name",
                "$.capability.exposes[?(@.type == 'mcp')].tools[*].steps[*].name",
                "$.capability.exposes[?(@.type == 'control')].address",
                "$.consumes[*]",
                "$.capability.consumes[*]"
        })
        @DisplayName("Expression parses without error")
        void naftikoExpressionValid(String expression) {
            assertParseable(naftikoDoc, expression);
        }
    }

    @Nested
    @DisplayName("Cross-cutting — All OAS expressions parse successfully")
    class OasExpressionValidity {

        @ParameterizedTest
        @ValueSource(strings = {
                "$",
                "$.info",
                "$.info.contact",
                "$.info.description",
                "$.info.license",
                "$.info.license.url",
                "$.paths",
                "$.paths[*]",
                "$.tags[*]",
                "$.tags",
                "$.servers[*]",
                "$.servers[*].url",
                "$.security[*]",
                "$.components.examples[*]",
                "$.components.parameters[*]",
                "$.components.responses[*]",
                "$.components.links[*]",
                "$.components.schemas..[?(@ && @.enum)]",
                // "$..[description,title]", // DIVERGENCE: bare identifiers not supported
                "$..description",
                "$..anyOf",
                "$..oneOf",
                "$..parameters[*]",
                "$..parameters[?(@ && @.in)]",
                "$..[?(@ && @.enum && @.type)]",
                "$..[?(@ && @.type=='array')]",
                "$.paths[*]['get','put','post','delete','options','head','patch','trace'].operationId",
                "$.paths[*]['get','post','delete'].description",
                "$.paths[*]['get','post','delete'].parameters[*]",
                "$.paths[*]['get','post','delete'].responses[*]",
                "$.paths[*]['get','post','delete'].security[*]",
                "$.paths[*]['get','post','delete'].tags",
                "$.paths[*].servers[*]",
                "$.paths[*]['get','post','delete'].servers[*]",
                "$.paths[*]..content[*].examples[*]",
                "$.paths[*]..parameters[*].examples[*]",
                "$.components.headers[*].examples[*]",
                "$..headers..[?(@ && @.schema)]",
                "$.definitions[?(@.discriminator)]",
                "$.webhooks.servers",
                "$.webhooks[*][*].servers",
                "$.webhooks[*][*].callbacks"
        })
        @DisplayName("Expression parses without error")
        void oasExpressionValid(String expression) {
            assertParseable(oasDoc, expression);
        }
    }
}
