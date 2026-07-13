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
package io.polychro.html;

import com.fasterxml.jackson.databind.JsonNode;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTableProjectionTest {

    private final HtmlParserFacade parserFacade = new HtmlParserFacade();
    private final HtmlProjector projector = new HtmlProjector();

    @Test
    void projectShouldNormalizeHeadedTableRowsByHeaderNames() {
        String html = """
                <table>
                  <thead><tr><th>Name</th><th>Age</th></tr></thead>
                  <tbody>
                    <tr><td>Alice</td><td>30</td></tr>
                    <tr><td>Bob</td></tr>
                  </tbody>
                </table>
                """;
        HtmlParseResult parsed = parserFacade.parse(html, HtmlParseResult.MODE_FRAGMENT);
        Document doc = projector.project(parsed, null);
        JsonNode table = doc.root().get("document").get("tables").get(0);
        assertEquals(2, table.get("headers").size());
        assertEquals("Name", table.get("headers").get(0).asText());
        assertEquals(2, table.get("rows").size());
        assertEquals("Alice", table.get("rows").get(0).get("Name").asText());
        assertEquals("30", table.get("rows").get(0).get("Age").asText());
        assertEquals("Bob", table.get("rows").get(1).get("Name").asText());
        assertEquals("", table.get("rows").get(1).get("Age").asText());
    }

    @Test
    void projectShouldFallBackToFirstRowThWhenNoThead() {
        String html = """
                <table>
                  <tr><th>Col1</th><th>Col2</th></tr>
                  <tr><td>a</td><td>b</td></tr>
                </table>
                """;
        HtmlParseResult parsed = parserFacade.parse(html, HtmlParseResult.MODE_FRAGMENT);
        Document doc = projector.project(parsed, null);
        JsonNode table = doc.root().get("document").get("tables").get(0);
        assertEquals(2, table.get("headers").size());
        assertEquals(1, table.get("rows").size());
        assertEquals("a", table.get("rows").get(0).get("Col1").asText());
    }

    @Test
    void projectShouldEmitNoRowsWhenHeadersMissing() {
        HtmlParseResult parsed = parserFacade.parse(
                "<table><tr><td>x</td></tr></table>", HtmlParseResult.MODE_FRAGMENT);
        Document doc = projector.project(parsed, null);
        JsonNode table = doc.root().get("document").get("tables").get(0);
        assertEquals(0, table.get("headers").size());
        assertEquals(0, table.get("rows").size());
    }

    @Test
    void projectShouldSkipRowsWithoutTdCells() {
        String html = """
                <table>
                  <thead><tr><th>A</th></tr></thead>
                  <tbody>
                    <tr></tr>
                    <tr><td>x</td></tr>
                  </tbody>
                </table>
                """;
        HtmlParseResult parsed = parserFacade.parse(html, HtmlParseResult.MODE_FRAGMENT);
        Document doc = projector.project(parsed, null);
        JsonNode table = doc.root().get("document").get("tables").get(0);
        assertEquals(1, table.get("rows").size());
        assertEquals("x", table.get("rows").get(0).get("A").asText());
    }

    @Test
    void projectShouldFallBackToAnyTrWhenNoTbody() {
        String html = """
                <table>
                  <thead><tr><th>A</th></tr></thead>
                  <tr><td>x</td></tr>
                </table>
                """;
        HtmlParseResult parsed = parserFacade.parse(html, HtmlParseResult.MODE_FRAGMENT);
        Document doc = projector.project(parsed, null);
        JsonNode table = doc.root().get("document").get("tables").get(0);
        assertTrue(table.get("rows").size() >= 1);
    }
}
