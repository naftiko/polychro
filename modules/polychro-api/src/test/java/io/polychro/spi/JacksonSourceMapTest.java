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

import static org.junit.jupiter.api.Assertions.*;

class JacksonSourceMapTest {

    // ---------------------------------------------------------------------
    // Golden fixture for issue #32 — YAML-only iso-Spectral source ranges.
    //
    // Each scalar's expected range is the exact 0-based range produced by
    // Spectral (npx @stoplight/spectral-cli) on the SAME bytes, captured once
    // and frozen here as the contract Polychro must replicate. Both the start
    // and the end are asserted (end exclusive, opening AND closing quotes
    // included for quoted scalars). These ranges are now produced by
    // JacksonSourceMap.toRange(), which spans each scalar's full end (#32).
    //
    // Layout (1-based human line numbers):
    //   1  info:
    //   2    unquoted: not-semver
    //   3    quoted: "hello"
    //   4    escaped: "a\tb"
    //   5    block: >
    //   6      first
    //   7      second
    // ---------------------------------------------------------------------
    private static final String GOLDEN_YAML =
            "info:\n"
            + "  unquoted: not-semver\n"
            + "  quoted: \"hello\"\n"
            + "  escaped: \"a\\tb\"\n"
            + "  block: >\n"
            + "    first\n"
            + "    second\n";

    // ------------------------------------------------------------------
    // PROVENANCE OF THE 0-BASED TARGETS BELOW — measured with Spectral, the
    // reference linter Polychro must replicate. To reproduce (Spectral CLI):
    //
    //   # golden.yaml = the exact bytes of GOLDEN_YAML above
    //   # ruleset.yaml = one `falsy` rule per scalar so each path is reported:
    //   #   rules:
    //   #     block-probe:    { given: "$.info.block",    severity: error, then: { function: falsy } }
    //   #     unquoted-probe: { given: "$.info.unquoted", severity: error, then: { function: falsy } }
    //   #     quoted-probe:   { given: "$.info.quoted",   severity: error, then: { function: falsy } }
    //   #     escaped-probe:  { given: "$.info.escaped",  severity: error, then: { function: falsy } }
    //   npx -y @stoplight/spectral-cli lint golden.yaml --ruleset ruleset.yaml --format json
    //
    // Spectral emits 0-based ranges, end exclusive. Captured output:
    //   unquoted  start=(1,12) end=(1,22)   # value hugs end-of-line
    //   quoted    start=(2,10) end=(2,17)   # both quotes included
    //   escaped   start=(3,11) end=(3,17)   # physical chars, not unescaped
    //   block     start=(4,9)  end=(6,11)   # '>' .. last content line + the
    //                                       # trailing newline that belongs to
    //                                       # the block scalar (col 10 = after
    //                                       # "second", +1 for the folded '\n')
    // ------------------------------------------------------------------
    // unquoted scalar "not-semver" — line 2 (0-based 1), value spans col 12..22.
    private static final int UNQUOTED_START_LINE = 1;
    private static final int UNQUOTED_START_COL = 12;
    private static final int UNQUOTED_END_LINE = 1;
    private static final int UNQUOTED_END_COL = 22;

    // quoted scalar "hello" — line 3 (0-based 2), range includes BOTH quotes: col 10..17.
    private static final int QUOTED_START_LINE = 2;
    private static final int QUOTED_START_COL = 10;
    private static final int QUOTED_END_LINE = 2;
    private static final int QUOTED_END_COL = 17;

    // escaped scalar "a\tb" — line 4 (0-based 3), physical chars counted: col 11..17.
    private static final int ESCAPED_START_LINE = 3;
    private static final int ESCAPED_START_COL = 11;
    private static final int ESCAPED_END_LINE = 3;
    private static final int ESCAPED_END_COL = 17;

    // block scalar '>' — starts line 5 (0-based 4) col 9; ends line 7 (0-based 6)
    // col 11 = end of "second" (col 10) + the trailing newline folded into the block.
    private static final int BLOCK_START_LINE = 4;
    private static final int BLOCK_START_COL = 9;
    private static final int BLOCK_END_LINE = 6;
    private static final int BLOCK_END_COL = 11;

    @Test
    void forContentShouldProduceIsoSpectralStartForUnquotedScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.unquoted");
        assertNotNull(range, "unquoted scalar must be located");
        assertEquals(UNQUOTED_START_LINE, range.startLine(), "0-based start line (iso-Spectral)");
        assertEquals(UNQUOTED_START_COL, range.startColumn(), "0-based start column (iso-Spectral)");
    }

    @Test
    void forContentShouldProduceIsoSpectralEndForUnquotedScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.unquoted");
        assertNotNull(range, "unquoted scalar must be located");
        assertEquals(UNQUOTED_END_LINE, range.endLine(), "0-based end line (iso-Spectral)");
        assertEquals(UNQUOTED_END_COL, range.endColumn(), "0-based exclusive end column (iso-Spectral)");
    }

    @Test
    void forContentShouldStartAtOpeningQuoteForQuotedScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.quoted");
        assertNotNull(range, "quoted scalar must be located");
        assertEquals(QUOTED_START_LINE, range.startLine());
        assertEquals(QUOTED_START_COL, range.startColumn(), "range starts at the opening quote, iso-Spectral");
    }

    @Test
    void forContentShouldIncludeBothQuotesInRangeForQuotedScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.quoted");
        assertNotNull(range, "quoted scalar must be located");
        assertEquals(QUOTED_END_LINE, range.endLine());
        assertEquals(QUOTED_END_COL, range.endColumn(), "range ends just past the closing quote, iso-Spectral");
    }

    @Test
    void forContentShouldStartAtOpeningQuoteForEscapedScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.escaped");
        assertNotNull(range, "escaped scalar must be located");
        assertEquals(ESCAPED_START_LINE, range.startLine());
        assertEquals(ESCAPED_START_COL, range.startColumn());
    }

    @Test
    void forContentShouldCountPhysicalCharactersForEscapedScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.escaped");
        assertNotNull(range, "escaped scalar must be located");
        assertEquals(ESCAPED_END_LINE, range.endLine());
        assertEquals(ESCAPED_END_COL, range.endColumn(), "physical chars (\\t counts as 2), iso-Spectral");
    }

    @Test
    void forContentShouldStartAtBlockIndicatorForBlockScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.block");
        assertNotNull(range, "block scalar must be located");
        assertEquals(BLOCK_START_LINE, range.startLine());
        assertEquals(BLOCK_START_COL, range.startColumn());
    }

    @Test
    void forContentShouldSpanMultipleLinesForBlockScalarOnYaml() {
        SourceMap map = JacksonSourceMap.forContent(GOLDEN_YAML, "yaml");

        SourceRange range = map.resolve("$.info.block");
        assertNotNull(range, "block scalar must be located");
        assertEquals(BLOCK_END_LINE, range.endLine(), "block range ends at the last content line, not Jackson's overshoot");
        assertEquals(BLOCK_END_COL, range.endColumn());
    }

    @Test
    void forContentShouldLocateScalarInsideArrayObjectOnYaml() {
        String yaml = "consumes:\n  - name: api\n    baseUri: \"https://example.com/\"\n";
        SourceMap map = JacksonSourceMap.forContent(yaml, "yaml");

        SourceRange range = map.resolve("$.consumes[0].baseUri");
        assertNotNull(range, "baseUri must be located");
        // 0-based: human line 3 -> index 2. The quoted value "https://example.com/" spans
        // col 13 (opening quote) .. 35 (exclusive, just past the closing quote). Asserting
        // both columns guards column computation for array-nested scalars, consistent with
        // the rest of the golden fixture (#32, review feedback).
        assertEquals(2, range.startLine());
        assertEquals(2, range.endLine());
        assertEquals(13, range.startColumn(), "range starts at the opening quote, iso-Spectral");
        assertEquals(35, range.endColumn(), "range ends just past the closing quote, iso-Spectral");
        assertTrue(range.endColumn() > range.startColumn(),
                "array-nested scalar must carry a real column span, not a degenerate point");
    }

    @Test
    void forContentShouldLocateTopLevelFieldOnYaml() {
        String yaml = "name: hello\nother: world\n";
        SourceMap map = JacksonSourceMap.forContent(yaml, "yaml");

        SourceRange first = map.resolve("$.name");
        SourceRange second = map.resolve("$.other");
        assertNotNull(first);
        assertNotNull(second);
        // 0-based: human lines 1 and 2 -> indices 0 and 1.
        assertEquals(0, first.startLine());
        assertEquals(1, second.startLine());
    }

    @Test
    void forContentShouldLocateNestedScalarsOnJson() {
        String json = "{\n  \"info\": {\n    \"name\": \"svc\"\n  }\n}";
        SourceMap map = JacksonSourceMap.forContent(json, "json");

        SourceRange range = map.resolve("$.info.name");
        assertNotNull(range);
        // JSON is out of scope for iso-Spectral end ranges (#34); only the
        // 0-based start line is asserted here. Human line 3 -> index 2.
        assertEquals(2, range.startLine());
    }

    @Test
    void forContentShouldLocateMultipleArrayElementsByIndex() {
        String yaml = "items:\n  - a\n  - b\n  - c\n";
        SourceMap map = JacksonSourceMap.forContent(yaml, "yaml");

        // 0-based: human lines 2,3,4 -> indices 1,2,3.
        assertEquals(1, map.resolve("$.items[0]").startLine());
        assertEquals(2, map.resolve("$.items[1]").startLine());
        assertEquals(3, map.resolve("$.items[2]").startLine());
    }

    @Test
    void resolveShouldReturnNullForUnknownPath() {
        SourceMap map = JacksonSourceMap.forContent("name: hello\n", "yaml");
        assertNull(map.resolve("$.does.not.exist"));
    }

    @Test
    void resolveShouldReturnNullForNullPath() {
        SourceMap map = JacksonSourceMap.forContent("name: hello\n", "yaml");
        assertNull(map.resolve(null));
    }

    @Test
    void forContentShouldReturnNoneForUnstructuredFormat() {
        assertSame(SourceMap.NONE, JacksonSourceMap.forContent("# heading\n", "markdown"));
    }

    @Test
    void forContentShouldReturnNoneForNullContent() {
        assertSame(SourceMap.NONE, JacksonSourceMap.forContent(null, "yaml"));
    }

    @Test
    void resolveShouldReturnNullForEmptyContent() {
        // An empty document has no nodes to locate; every lookup yields null (no range),
        // which callers treat exactly like an unresolved path — no NPE, no spurious range.
        SourceMap map = JacksonSourceMap.forContent("", "yaml");
        assertNull(map.resolve("$"));
    }

    @Test
    void forContentShouldReturnNoneForUnparseableContent() {
        // A malformed JSON document cannot be scanned for locations; the map degrades to NONE.
        SourceMap map = JacksonSourceMap.forContent("{ \"broken\": ", "json");
        assertSame(SourceMap.NONE, map);
    }

    @Test
    void forContentShouldLocateRootScalar() {
        // A bare scalar document maps to the root path "$".
        SourceMap map = JacksonSourceMap.forContent("\"just-a-string\"", "json");
        assertNotNull(map.resolve("$"));
        // 0-based: human line 1 -> index 0.
        assertEquals(0, map.resolve("$").startLine());
    }
}
