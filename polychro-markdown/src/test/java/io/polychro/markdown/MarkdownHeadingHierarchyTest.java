package io.polychro.markdown;

import com.fasterxml.jackson.databind.node.TextNode;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownHeadingHierarchyTest {

    private final MarkdownValidator validator = new MarkdownValidator(
            120, "-", new GenericFormat(), new FrontmatterParser());

    private Document doc(String content) {
        return new Document(new TextNode(content), "test.md");
    }

    @Test
    void validateShouldAcceptSequentialHeadings() {
        String content = "# Title\n\n## Section\n\n### Subsection\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("heading-hierarchy")));
    }

    @Test
    void validateShouldReportSkipFromH1ToH3() {
        String content = "# Title\n\n### Skipped\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("heading-hierarchy")));
    }

    @Test
    void validateShouldAcceptMultipleSiblingH2() {
        String content = "# Title\n\n## First\n\n## Second\n\n## Third\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("heading-hierarchy")));
    }

    @Test
    void validateShouldReportSkipFromH2ToH4() {
        String content = "# Title\n\n## Section\n\n#### Deep\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().anyMatch(d -> d.code().equals("heading-hierarchy")));
    }

    @Test
    void validateShouldAcceptSingleHeading() {
        String content = "# Title\n";
        List<Diagnostic> result = validator.validate(doc(content));
        assertTrue(result.stream().noneMatch(d -> d.code().equals("heading-hierarchy")));
    }
}
