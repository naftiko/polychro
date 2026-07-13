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
package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLineLengthTest {

    private MarkdownValidator validator(int lineLength) {
        return new MarkdownValidator(lineLength, "-", new GenericFormat(), new FrontmatterParser());
    }

    private Document doc(String content) {
        return new Document(new TextNode(content), "test.md");
    }

    @Test
    void validateShouldNotReportLineAtLimit() {
        String line = "a".repeat(120);
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(120).validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldReportLineOverLimit() {
        String line = "a".repeat(121);
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(120).validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldIgnoreUrlOnlyLine() {
        String line = "https://example.com/" + "a".repeat(200);
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(120).validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldIgnoreTableRow() {
        String line = "| " + "a".repeat(200) + " |";
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(120).validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldIgnoreIndentedCodeLine() {
        String line = "    " + "a".repeat(200);
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(120).validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldUseCustomLimit() {
        String line = "a".repeat(81);
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(80).validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("line-too-long")));
    }

    @Test
    void validateShouldReportCorrectLineNumber() {
        String line = "a".repeat(121);
        // line is on line 3 (1=heading, 2=blank, 3=long line)
        String content = "# Title\n\n" + line + "\n";
        List<Diagnostic> result = validator(120).validate(doc(content));
        Diagnostic d = result.stream().filter(diag -> diag.code().equals("line-too-long")).findFirst().orElseThrow();
        assertEquals(3, d.range().startLine());
    }
}
