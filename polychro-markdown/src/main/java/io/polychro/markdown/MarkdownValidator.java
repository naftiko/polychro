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
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

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

    private static final Parser PARSER = Parser.builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();
    private static final Pattern SLUG_SPECIAL = Pattern.compile("[^a-z0-9 -]");

    private final int lineLength;
    private final String listMarker;
    private final MarkdownFormat format;
    private final FrontmatterParser frontmatterParser;
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

        // Parse frontmatter
        FrontmatterResult frontmatter = frontmatterParser.parse(content);
        if (frontmatter.hasError()) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "frontmatter-parse-error",
                    frontmatter.errorMessage(), null,
                    new SourceRange(1, 1, 1, 1)));
        }

        // Parse CommonMark AST (body only, excluding frontmatter)
        String body = frontmatter.body();
        int bodyStartLine = frontmatter.bodyStartLine();
        Node document = PARSER.parse(body);

        // Structural checks
        checkHeadingHierarchy(document, bodyStartLine, diagnostics);
        checkDuplicateAnchors(document, bodyStartLine, diagnostics);
        checkInternalLinks(document, bodyStartLine, diagnostics);

        // Formatting checks
        checkCodeBlockLanguage(document, bodyStartLine, diagnostics);
        checkLineLength(lines, diagnostics);
        checkTrailingWhitespace(lines, diagnostics);
        checkListMarkers(lines, frontmatter.bodyStartLine(), diagnostics);
        checkBlankLineBeforeHeading(lines, diagnostics);

        // Relative and external link checks
        checkFileLinks(document, bodyStartLine, doc.sourcePath(), diagnostics);

        // Format-specific checks
        format.validate(doc, frontmatter, diagnostics);

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

    void checkHeadingHierarchy(Node document, int bodyStartLine, List<Diagnostic> diagnostics) {
        List<HeadingInfo> headings = collectHeadings(document, bodyStartLine);
        for (int i = 1; i < headings.size(); i++) {
            HeadingInfo prev = headings.get(i - 1);
            HeadingInfo curr = headings.get(i);
            if (curr.level() > prev.level() + 1) {
                diagnostics.add(new Diagnostic(Severity.WARN, "heading-hierarchy",
                        "Heading level skipped: expected h" + (prev.level() + 1)
                                + " or lower, found h" + curr.level(),
                        null,
                        new SourceRange(curr.line(), 1, curr.line(), 1)));
            }
        }
    }

    void checkDuplicateAnchors(Node document, int bodyStartLine, List<Diagnostic> diagnostics) {
        List<HeadingInfo> headings = collectHeadings(document, bodyStartLine);
        Set<String> seen = new HashSet<>();
        for (HeadingInfo heading : headings) {
            String slug = slugify(heading.text());
            if (!seen.add(slug)) {
                diagnostics.add(new Diagnostic(Severity.WARN, "duplicate-anchor",
                        "Duplicate heading anchor: #" + slug,
                        null,
                        new SourceRange(heading.line(), 1, heading.line(), 1)));
            }
        }
    }

    void checkInternalLinks(Node document, int bodyStartLine, List<Diagnostic> diagnostics) {
        List<HeadingInfo> headings = collectHeadings(document, bodyStartLine);
        Set<String> anchors = new HashSet<>();
        Map<String, Integer> anchorCounts = new HashMap<>();
        for (HeadingInfo heading : headings) {
            String slug = slugify(heading.text());
            anchors.add(slug);
            anchorCounts.merge(slug, 1, Integer::sum);
        }

        List<LinkInfo> links = collectInternalLinks(document, bodyStartLine);
        for (LinkInfo link : links) {
            // link.target() always starts with '#' (guaranteed by collectInternalLinks)
            String target = link.target().substring(1);
            String normalizedTarget = target.toLowerCase();
            if (!anchors.contains(normalizedTarget)) {
                diagnostics.add(new Diagnostic(Severity.WARN, "broken-internal-link",
                        "Internal link target not found: #" + target,
                        null,
                        new SourceRange(link.line(), 1, link.line(), 1)));
            }
        }
    }

    void checkCodeBlockLanguage(Node document, int bodyStartLine, List<Diagnostic> diagnostics) {
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(FencedCodeBlock fencedCodeBlock) {
                String info = fencedCodeBlock.getInfo();
                if (info == null || info.isBlank()) {
                    int line = getNodeLine(fencedCodeBlock, bodyStartLine);
                    diagnostics.add(new Diagnostic(Severity.INFO, "code-block-no-language",
                            "Fenced code block has no language annotation",
                            null,
                            new SourceRange(line, 1, line, 1)));
                }
            }
        });
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

    List<HeadingInfo> collectHeadings(Node document, int bodyStartLine) {
        List<HeadingInfo> headings = new ArrayList<>();
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                String text = extractText(heading);
                int line = getNodeLine(heading, bodyStartLine);
                headings.add(new HeadingInfo(heading.getLevel(), text, line));
            }
        });
        return headings;
    }

    List<LinkInfo> collectInternalLinks(Node document, int bodyStartLine) {
        List<LinkInfo> links = new ArrayList<>();
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Link link) {
                String destination = link.getDestination();
                if (destination != null && destination.startsWith("#")) {
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

    record HeadingInfo(int level, String text, int line) {}

    record LinkInfo(String target, int line) {}
}
