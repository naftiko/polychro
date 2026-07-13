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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Branch-level coverage for the YAML end-scan helpers of {@link JacksonSourceMap}. The golden
 * fixture in {@code JacksonSourceMapTest} exercises the common path (one plain, one double-quoted,
 * one escaped, one folded block scalar); these tests cover the edge cases that fixture cannot
 * express without losing its iso-Spectral readability: single quotes, {@code ''} doubling,
 * unterminated quotes, trailing comments, literal {@code |} blocks, blank lines inside a block, and
 * out-of-bounds start positions.
 */
class JacksonSourceMapEndScanTest {

    @Test
    void endOfYamlScalarShouldReturnStartWhenStartLineIsNegative() {
        assertArrayEquals(new int[] {-1, 4}, JacksonSourceMap.endOfYamlScalar("a: b\n", -1, 4));
    }

    @Test
    void endOfYamlScalarShouldReturnStartWhenStartLineIsBeyondContent() {
        assertArrayEquals(new int[] {9, 0}, JacksonSourceMap.endOfYamlScalar("a: b\n", 9, 0));
    }

    @Test
    void endOfYamlScalarShouldReturnStartWhenStartColumnIsNegative() {
        assertArrayEquals(new int[] {0, -1}, JacksonSourceMap.endOfYamlScalar("a: b\n", 0, -1));
    }

    @Test
    void endOfYamlScalarShouldReturnStartWhenStartColumnIsBeyondLine() {
        assertArrayEquals(new int[] {0, 99}, JacksonSourceMap.endOfYamlScalar("a: b\n", 0, 99));
    }

    @Test
    void endOfYamlScalarShouldTreatEndOfLineAsAnEmptyScalar() {
        // startColumn == line length is in bounds; first char is '\0' (plain path), end == start.
        assertArrayEquals(new int[] {0, 4}, JacksonSourceMap.endOfYamlScalar("a: b", 0, 4));
    }

    @Test
    void endOfYamlScalarShouldDispatchToQuotedForSingleQuote() {
        int[] end = JacksonSourceMap.endOfYamlScalar("k: 'hi'\n", 0, 3);
        assertArrayEquals(new int[] {0, 7}, end, "single-quoted scalar ends past the closing quote");
    }

    @Test
    void endOfYamlScalarShouldDispatchToBlockForLiteralIndicator() {
        int[] end = JacksonSourceMap.endOfYamlScalar("k: |\n  one\n", 0, 3);
        assertArrayEquals(new int[] {1, 6}, end, "literal '|' block spans its content line + newline");
    }

    @Test
    void endOfQuotedScalarShouldSkipEscapedDoubleQuote() {
        // "a\"b" — the escaped quote is not the terminator; closing quote is the last char.
        String line = "k: \"a\\\"b\"";
        int[] end = JacksonSourceMap.endOfQuotedScalar(line, 0, 3, '"');
        assertEquals(line.length(), end[1], "escaped \\\" is skipped, range ends past the real close");
    }

    @Test
    void endOfQuotedScalarShouldSkipDoubledSingleQuote() {
        // 'a''b' — the doubled '' is a literal quote, not the terminator.
        String line = "k: 'a''b'";
        int[] end = JacksonSourceMap.endOfQuotedScalar(line, 0, 3, '\'');
        assertEquals(line.length(), end[1], "'' doubling is skipped, range ends past the real close");
    }

    @Test
    void endOfQuotedScalarShouldFallBackToEndOfLineWhenUnterminated() {
        String line = "k: \"unterminated";
        int[] end = JacksonSourceMap.endOfQuotedScalar(line, 0, 3, '"');
        assertArrayEquals(new int[] {0, line.length()}, end, "missing close quote falls back to EOL");
    }

    @Test
    void endOfPlainScalarShouldStopBeforeTrailingComment() {
        String line = "k: value # comment";
        int[] end = JacksonSourceMap.endOfPlainScalar(line, 0, 3);
        assertArrayEquals(new int[] {0, 8}, end, "range hugs 'value', the ' # comment' is excluded");
    }

    @Test
    void endOfPlainScalarShouldStopBeforeEmptyTrailingComment() {
        // A '#' that is the very last character of the line is still a comment marker:
        // the range must hug 'value', not include the trailing ' #'. Iso-Spectral end column = 8.
        String line = "k: value #";
        int[] end = JacksonSourceMap.endOfPlainScalar(line, 0, 3);
        assertArrayEquals(new int[] {0, 8}, end, "terminal '#' is excluded, range hugs 'value'");
    }

    @Test
    void endOfPlainScalarShouldStopBeforeEmptyTrailingCommentWithExtraSpace() {
        // Two spaces before a terminal '#': the comment marker is the last character and must
        // still be detected, so the trailing '  #' is excluded. Iso-Spectral end column = 8.
        String line = "k: value  #";
        int[] end = JacksonSourceMap.endOfPlainScalar(line, 0, 3);
        assertArrayEquals(new int[] {0, 8}, end, "terminal '#' after extra space is excluded");
    }

    @Test
    void endOfPlainScalarShouldKeepTerminalHashGluedToValue() {
        // A '#' glued to the value (no preceding whitespace) is part of the value, not a comment
        // marker — even as the line's last character under the widened loop bound. The range must
        // include the '#', so end == line.length() (the value is "value#").
        String line = "k: value#";
        int[] end = JacksonSourceMap.endOfPlainScalar(line, 0, 3);
        assertArrayEquals(new int[] {0, 9}, end, "glued terminal '#' is part of the value, not a comment");
    }

    @Test
    void endOfPlainScalarShouldTrimTrailingWhitespace() {
        String line = "k: value   ";
        int[] end = JacksonSourceMap.endOfPlainScalar(line, 0, 3);
        assertArrayEquals(new int[] {0, 8}, end, "trailing spaces are excluded from the range");
    }

    @Test
    void endOfBlockScalarShouldSkipBlankLinesInsideContent() {
        String[] lines = "k: >\n  one\n\n  two\n".split("\n", -1);
        int[] end = JacksonSourceMap.endOfBlockScalar(lines, 0, 3);
        assertArrayEquals(new int[] {3, 6}, end, "blank line is skipped, end is past 'two' + newline");
    }

    @Test
    void endOfBlockScalarShouldStopAtLessIndentedLine() {
        String[] lines = "k: >\n  one\nnext: x\n".split("\n", -1);
        int[] end = JacksonSourceMap.endOfBlockScalar(lines, 0, 3);
        assertArrayEquals(new int[] {1, 6}, end, "dedented key terminates the block, end past 'one'");
    }

    @Test
    void indentOfShouldReturnZeroForUnindentedLine() {
        assertEquals(0, JacksonSourceMap.indentOf("key: value"));
    }

    @Test
    void indentOfShouldCountLeadingSpaces() {
        assertEquals(4, JacksonSourceMap.indentOf("    key: value"));
    }

    @Test
    void endOfQuotedScalarShouldNotConsumeTrailingBackslashAtEndOfLine() {
        // A backslash in the final position has no following char to escape, so the loop does not
        // jump past it; the scan falls through to the unterminated-quote end-of-line fallback.
        String line = "k: \"ab\\";
        int[] end = JacksonSourceMap.endOfQuotedScalar(line, 0, 3, '"');
        assertArrayEquals(new int[] {0, line.length()}, end, "lone trailing backslash, no close quote");
    }

    @Test
    void endOfPlainScalarShouldNotTreatHashGluedToTextAsComment() {
        // A '#' that is not preceded by whitespace is part of the value, not a comment marker.
        String line = "k: va#lue";
        int[] end = JacksonSourceMap.endOfPlainScalar(line, 0, 3);
        assertArrayEquals(new int[] {0, line.length()}, end, "glued '#' stays inside the value");
    }

    @Test
    void endOfBlockScalarShouldFallBackToStartWhenNoContentFollows() {
        // A block indicator with no following content line (end of document): the loop never runs,
        // so the end stays at the start column + 1.
        String[] lines = "k: >\n".split("\n", -1);
        int[] end = JacksonSourceMap.endOfBlockScalar(lines, 0, 3);
        assertArrayEquals(new int[] {0, 4}, end, "empty block falls back to startColumn + 1");
    }

    @Test
    void indentOfShouldCountAllSpacesOnABlankLine() {
        // A line made entirely of spaces exits the loop on the length bound, not on a non-space.
        assertEquals(3, JacksonSourceMap.indentOf("   "));
    }

    @Test
    void endOfQuotedScalarShouldEndAtClosingSingleQuoteFollowedByMoreText() {
        // The closing ' is followed by a non-quote char, so the doubling guard is false and the
        // scan terminates on the closing quote rather than treating it as an escaped pair.
        String line = "k: 'hi' x";
        int[] end = JacksonSourceMap.endOfQuotedScalar(line, 0, 3, '\'');
        assertArrayEquals(new int[] {0, 7}, end, "single-quoted scalar ends past its closing quote");
    }

    @Test
    void endOfBlockScalarShouldTrimTrailingWhitespaceOnAContentLine() {
        // A content line padded with trailing spaces: the inner trim loop must strip them so the
        // range hugs the text, not the padding.
        String[] lines = "k: >\n  one  \n".split("\n", -1);
        int[] end = JacksonSourceMap.endOfBlockScalar(lines, 0, 3);
        assertArrayEquals(new int[] {1, 6}, end, "trailing spaces trimmed, end past 'one' + newline");
    }

    @Test
    void endOfPlainScalarShouldNotThrowWhenStartColumnIsZero() {
        // A plain scalar reported at column 0 whose first char is '#': the comment-detection
        // back-reference (charAt(i - 1)) must not read charAt(-1). The '#' at the value's first
        // column is part of the value, not a trailing comment.
        int[] end = JacksonSourceMap.endOfYamlScalar("#x", 0, 0);
        assertArrayEquals(new int[] {0, 2}, end, "value starting with '#' at column 0 hugs the whole token");
    }
}
