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

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;
import io.polychro.spi.Validator;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates Markdown documents for structural and formatting correctness.
 */
class MarkdownValidator implements Validator {

    static final String NAME = "markdown";
    static final int DEFAULT_LINE_LENGTH = 120;
    static final String DEFAULT_LIST_MARKER = "-";
    static final int DEFAULT_EXTERNAL_LINK_TIMEOUT_MS = 5000;
    static final int DEFAULT_EXTERNAL_LINK_RATE_LIMIT = 10;

    private static final Pattern SLUG_SPECIAL = Pattern.compile("[^a-z0-9 -]");

    private final int lineLength;
    private final String listMarker;
    private final MarkdownFormat format;
    private final FrontmatterParser frontmatterParser;
        private final MarkdownParserFacade parserFacade;
        private final MarkdownProjector projector;
    private final boolean checkExternalLinks;
    private final RelativeLinkChecker relativeLinkChecker;
    private final RelativeAnchorChecker relativeAnchorChecker;
    private final ExternalLinkChecker externalLinkChecker;

    MarkdownValidator(int lineLength, String listMarker, MarkdownFormat format,
                      FrontmatterParser frontmatterParser, boolean checkExternalLinks,
                      int externalLinkTimeoutMs, int externalLinkRateLimit) {
        this.lineLength = lineLength;
        this.listMarker = listMarker;
        this.format = format;
        this.frontmatterParser = frontmatterParser;
        this.parserFacade = new MarkdownParserFacade(frontmatterParser);
        this.projector = new MarkdownProjector();
        this.checkExternalLinks = checkExternalLinks;
        this.relativeLinkChecker = new RelativeLinkChecker();
        this.relativeAnchorChecker = new RelativeAnchorChecker();
        this.externalLinkChecker = new ExternalLinkChecker(externalLinkTimeoutMs, externalLinkRateLimit);
    }

    MarkdownValidator(int lineLength, String listMarker, MarkdownFormat format,
                      FrontmatterParser frontmatterParser) {
        this(lineLength, listMarker, format, frontmatterParser, false,
                DEFAULT_EXTERNAL_LINK_TIMEOUT_MS, DEFAULT_EXTERNAL_LINK_RATE_LIMIT);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        String content = extractRawContent(doc);
        if (content == null) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "no-content",
                    "Markdown document has no text content", null, null));
            return diagnostics;
        }

        String[] lines = content.split("\n", -1);

        MarkdownParseResult parsed = parserFacade.parse(content);
        FrontmatterResult frontmatter = parsed.frontmatter();
        if (frontmatter.hasError()) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "frontmatter-parse-error",
                    frontmatter.errorMessage(), null,
                    new SourceRange(1, 1, 1, 1)));
        }

        Document projected = projector.project(parsed, doc.sourcePath());

        // Structural checks
        checkHeadingHierarchy(projected, diagnostics);
        checkDuplicateAnchors(projected, diagnostics);
        checkInternalLinks(projected, diagnostics);

        // Formatting checks
        checkCodeBlockLanguage(projected, diagnostics);
        checkLineLength(lines, diagnostics);
        checkTrailingWhitespace(lines, diagnostics);
        checkListMarkers(lines, frontmatter.bodyStartLine(), diagnostics);
        checkBlankLineBeforeHeading(lines, diagnostics);

        // Relative and external link checks
        checkFileLinks(parsed.bodyDocument(), parsed.bodyStartLine(), doc.sourcePath(), diagnostics);

        // Format-specific checks
        format.validate(projected, diagnostics);

        return diagnostics;
    }

    String extractRawContent(Document doc) {
        if (doc.root() == null) {
            return null;
        }
        if (doc.root().isTextual()) {
            return doc.root().asText();
        }
        return null;
    }

    void checkHeadingHierarchy(Document projected, List<Diagnostic> diagnostics) {
        List<ProjectedHeadingInfo> headings = collectProjectedHeadings(projected);
        for (int i = 1; i < headings.size(); i++) {
            ProjectedHeadingInfo prev = headings.get(i - 1);
            ProjectedHeadingInfo curr = headings.get(i);
            if (curr.level() > prev.level() + 1) {
                diagnostics.add(new Diagnostic(Severity.WARN, "heading-hierarchy",
                        "Heading level skipped: expected h" + (prev.level() + 1)
                                + " or lower, found h" + curr.level(),
                        null,
                        rangeFor(projected, curr.path())));
            }
        }
    }

    void checkDuplicateAnchors(Document projected, List<Diagnostic> diagnostics) {
        List<ProjectedHeadingInfo> headings = collectProjectedHeadings(projected);
        Set<String> seen = new HashSet<>();
        for (ProjectedHeadingInfo heading : headings) {
            String slug = slugify(heading.text());
            if (!seen.add(slug)) {
                diagnostics.add(new Diagnostic(Severity.WARN, "duplicate-anchor",
                        "Duplicate heading anchor: #" + slug,
                        null,
                        rangeFor(projected, heading.path())));
            }
        }
    }

    void checkInternalLinks(Document projected, List<Diagnostic> diagnostics) {
        List<ProjectedHeadingInfo> headings = collectProjectedHeadings(projected);
        Set<String> anchors = new HashSet<>();
        Map<String, Integer> anchorCounts = new HashMap<>();
        for (ProjectedHeadingInfo heading : headings) {
            String slug = slugify(heading.text());
            anchors.add(slug);
            anchorCounts.merge(slug, 1, Integer::sum);
        }

        List<ProjectedLinkInfo> links = collectProjectedInternalLinks(projected);
        for (ProjectedLinkInfo link : links) {
            // link.target() always starts with '#' (guaranteed by collectInternalLinks)
            String target = link.target().substring(1);
            String normalizedTarget = target.toLowerCase();
            if (!anchors.contains(normalizedTarget)) {
                diagnostics.add(new Diagnostic(Severity.WARN, "broken-internal-link",
                        "Internal link target not found: #" + target,
                        null,
                        rangeFor(projected, link.path())));
            }
        }
    }

    void checkCodeBlockLanguage(Document projected, List<Diagnostic> diagnostics) {
        JsonNode codeBlocks = projected.root().path("document").path("codeBlocks");
        for (int i = 0; i < codeBlocks.size(); i++) {
            JsonNode codeBlock = codeBlocks.get(i);
            JsonNode language = codeBlock.get("language");
            if (language == null || language.isNull() || language.asText().isBlank()) {
                diagnostics.add(new Diagnostic(Severity.INFO, "code-block-no-language",
                        "Fenced code block has no language annotation",
                        null,
                        rangeFor(projected, "$.document.codeBlocks[" + i + "]")));
            }
        }
    }

    void checkLineLength(String[] lines, List<Diagnostic> diagnostics) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() > lineLength) {
                if (isLineLengthExempt(line)) {
                    continue;
                }
                diagnostics.add(new Diagnostic(Severity.WARN, "line-too-long",
                        "Line exceeds " + lineLength + " characters (found " + line.length() + ")",
                        null,
                        new SourceRange(i + 1, lineLength + 1, i + 1, line.length())));
            }
        }
    }

    boolean isLineLengthExempt(String line) {
        String trimmed = line.trim();
        // URL-only lines
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true;
        }
        // Table rows
        if (trimmed.startsWith("|")) {
            return true;
        }
        // Code block content (indented by 4+ spaces or inside fenced block)
        // We check for 4-space indent as a heuristic for indented code blocks
        if (line.startsWith("    ") && !line.trim().startsWith("-") && !line.trim().startsWith("*")) {
            return true;
        }
        return false;
    }

    void checkTrailingWhitespace(String[] lines, List<Diagnostic> diagnostics) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty() && line.charAt(line.length() - 1) == ' ') {
                diagnostics.add(new Diagnostic(Severity.INFO, "trailing-whitespace",
                        "Trailing whitespace",
                        null,
                        new SourceRange(i + 1, line.length(), i + 1, line.length())));
            }
        }
    }

    void checkListMarkers(String[] lines, int bodyStartLine, List<Diagnostic> diagnostics) {
        for (int i = bodyStartLine - 1; i < lines.length; i++) {
            String trimmed = lines[i].stripLeading();
            if (trimmed.length() >= 2) {
                char first = trimmed.charAt(0);
                if ((first == '-' || first == '*' || first == '+') && trimmed.charAt(1) == ' ') {
                    String marker = String.valueOf(first);
                    if (!marker.equals(listMarker)) {
                        diagnostics.add(new Diagnostic(Severity.INFO, "inconsistent-list-marker",
                                "Expected list marker '" + listMarker + "' but found '" + marker + "'",
                                null,
                                new SourceRange(i + 1, 1, i + 1, 2)));
                    }
                }
            }
        }
    }

    void checkBlankLineBeforeHeading(String[] lines, List<Diagnostic> diagnostics) {
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("#") && !lines[i - 1].isBlank()) {
                // Don't flag if previous line is a frontmatter delimiter
                if (lines[i - 1].equals("---")) {
                    continue;
                }
                diagnostics.add(new Diagnostic(Severity.INFO, "no-blank-line-before-heading",
                        "Missing blank line before heading",
                        null,
                        new SourceRange(i + 1, 1, i + 1, 1)));
            }
        }
    }

    void checkFileLinks(Node document, int bodyStartLine, String sourcePath,
                        List<Diagnostic> diagnostics) {
        if (sourcePath == null) {
            return; // Cannot resolve relative links without a source path
        }

        Path docPath = Path.of(sourcePath);
        Path documentDir = docPath.getParent();
        if (documentDir == null) {
            return;
        }

        List<LinkInfo> allLinks = collectAllLinks(document, bodyStartLine);

        // Split into relative and external
        List<LinkInfo> relativeLinks = new ArrayList<>();
        List<LinkInfo> externalLinks = new ArrayList<>();

        for (LinkInfo link : allLinks) {
            String target = link.target();
            if (target.startsWith("#")) {
                continue; // Internal anchor — handled by checkInternalLinks
            }
            if (target.startsWith("http://") || target.startsWith("https://")) {
                externalLinks.add(link);
            } else if (!target.startsWith("mailto:") && !target.startsWith("tel:")) {
                relativeLinks.add(link);
            }
        }

        // Relative file link checks (always on)
        diagnostics.addAll(relativeLinkChecker.check(relativeLinks, documentDir));
        diagnostics.addAll(relativeAnchorChecker.check(relativeLinks, documentDir));

        // External link checks (opt-in)
        if (checkExternalLinks) {
            diagnostics.addAll(externalLinkChecker.check(externalLinks));
        }
    }

    List<LinkInfo> collectAllLinks(Node document, int bodyStartLine) {
        List<LinkInfo> links = new ArrayList<>();
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Link link) {
                String destination = link.getDestination();
                if (destination != null && !destination.isBlank()) {
                    int line = getNodeLine(link, bodyStartLine);
                    links.add(new LinkInfo(destination, line));
                }
                visitChildren(link);
            }
        });
        return links;
    }

    static String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override
            public void visit(Text text) {
                sb.append(text.getLiteral());
            }

            @Override
            public void visit(Code code) {
                sb.append(code.getLiteral());
            }
        });
        return sb.toString();
    }

    static String slugify(String text) {
        String lower = text.toLowerCase();
        String cleaned = SLUG_SPECIAL.matcher(lower).replaceAll("");
        return cleaned.replace(' ', '-');
    }

    static int getNodeLine(Node node, int bodyStartLine) {
        List<org.commonmark.node.SourceSpan> spans = node.getSourceSpans();
        if (!spans.isEmpty()) {
            return spans.getFirst().getLineIndex() + bodyStartLine;
        }
        return bodyStartLine;
    }

    List<ProjectedHeadingInfo> collectProjectedHeadings(Document projected) {
        List<ProjectedHeadingInfo> headings = new ArrayList<>();
        JsonNode headingNodes = projected.root().path("document").path("headings");
        for (int i = 0; i < headingNodes.size(); i++) {
            JsonNode heading = headingNodes.get(i);
            headings.add(new ProjectedHeadingInfo(
                    heading.path("level").asInt(),
                    heading.path("text").asText(),
                    "$.document.headings[" + i + "]"));
        }
        return headings;
    }

    List<ProjectedLinkInfo> collectProjectedInternalLinks(Document projected) {
        List<ProjectedLinkInfo> links = new ArrayList<>();
        JsonNode linkNodes = projected.root().path("document").path("links");
        for (int i = 0; i < linkNodes.size(); i++) {
            JsonNode link = linkNodes.get(i);
            if ("internal-anchor".equals(link.path("kind").asText())) {
                links.add(new ProjectedLinkInfo(
                        link.path("target").asText(),
                        "$.document.links[" + i + "]"));
            }
        }
        return links;
    }

    SourceRange rangeFor(Document projected, String path) {
        SourceRange range = projected.sourceMap().resolve(path);
        return range != null ? range : new SourceRange(1, 1, 1, 1);
    }

    record LinkInfo(String target, int line) {}

    record ProjectedHeadingInfo(int level, String text, String path) {}

    record ProjectedLinkInfo(String target, String path) {}
}
