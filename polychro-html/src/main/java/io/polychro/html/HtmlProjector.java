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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.polychro.spi.Document;
import io.polychro.spi.FormatProjector;
import io.polychro.spi.SourceRange;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Projects parsed HTML into the canonical document model.
 */
class HtmlProjector implements FormatProjector<HtmlParseResult> {

    private static final List<String> HEADING_TAGS = List.of("h1", "h2", "h3", "h4", "h5", "h6");

    @Override
    public String format() {
        return "html";
    }

    @Override
    public Document project(HtmlParseResult parsed, String sourcePath) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ObjectNode document = root.putObject("document");
        HtmlSourceMapBuilder sourceMapBuilder = new HtmlSourceMapBuilder();

        document.put("kind", HtmlParseResult.MODE_FRAGMENT.equals(parsed.mode())
                ? "html-fragment" : "html-document");
        document.put("mode", parsed.mode());

        Element htmlRoot = parsed.document();
        document.put("lang", extractLang(htmlRoot));
        document.put("title", extractTitle(htmlRoot));

        ArrayNode nodes = document.putArray("nodes");
        ArrayNode headings = document.putArray("headings");
        ArrayNode links = document.putArray("links");
        ArrayNode tables = document.putArray("tables");
        ArrayNode forms = document.putArray("forms");
        ArrayNode scripts = document.putArray("scripts");

        Element start = HtmlParseResult.MODE_FRAGMENT.equals(parsed.mode())
                ? parsed.document().body()
                : parsed.document();

        appendNodes(start, nodes, "$.document.nodes", sourceMapBuilder);

        collectHeadings(parsed.document(), headings, sourceMapBuilder);
        collectLinks(parsed.document(), links, sourceMapBuilder);
        collectTables(parsed.document(), tables, sourceMapBuilder);
        collectForms(parsed.document(), forms, sourceMapBuilder);
        collectScripts(parsed.document(), scripts, sourceMapBuilder);

        return new Document(root, format(), sourcePath, sourceMapBuilder.build(),
                Map.of("range.precision", HtmlSourceMapBuilder.PRECISION_APPROXIMATE));
    }

    private String extractLang(Element root) {
        Element htmlEl = root.selectFirst("html");
        String lang = htmlEl.attr("lang");
        return lang.isEmpty() ? null : lang;
    }

    private String extractTitle(Element root) {
        Element titleEl = root.selectFirst("title");
        if (titleEl == null) {
            return null;
        }
        String text = titleEl.text().trim();
        return text.isEmpty() ? null : text;
    }

    private void appendNodes(Element parent, ArrayNode nodes, String basePath,
                             HtmlSourceMapBuilder sourceMapBuilder) {
        for (Element child : parent.children()) {
            int index = nodes.size();
            String path = basePath + "[" + index + "]";
            ObjectNode node = nodes.addObject();
            node.put("tag", child.tagName().toLowerCase(Locale.ROOT));
            String id = child.id();
            if (!id.isEmpty()) {
                node.put("id", id);
            }
            String text = child.ownText().trim();
            if (!text.isEmpty()) {
                node.put("text", text);
            }
            ObjectNode attrs = node.putObject("attributes");
            for (Attribute attr : child.attributes()) {
                attrs.put(attr.getKey(), attr.getValue());
            }
            sourceMapBuilder.put(path, rangeFor(child), precisionFor(child));
            ArrayNode childArray = node.putArray("children");
            appendNodes(child, childArray, path + ".children", sourceMapBuilder);
        }
    }

    private void collectHeadings(Element root, ArrayNode headings, HtmlSourceMapBuilder sourceMapBuilder) {
        int index = 0;
        for (Element el : root.getAllElements()) {
            String tag = el.tagName().toLowerCase(Locale.ROOT);
            if (!HEADING_TAGS.contains(tag)) {
                continue;
            }
            ObjectNode entry = headings.addObject();
            entry.put("tag", tag);
            entry.put("level", Integer.parseInt(tag.substring(1)));
            entry.put("text", el.text().trim());
            String id = el.id();
            if (!id.isEmpty()) {
                entry.put("id", id);
            }
            sourceMapBuilder.put("$.document.headings[" + index + "]", rangeFor(el), precisionFor(el));
            index++;
        }
    }

    private void collectLinks(Element root, ArrayNode links, HtmlSourceMapBuilder sourceMapBuilder) {
        int index = 0;
        for (Element el : root.select("a[href], link[href]")) {
            ObjectNode entry = links.addObject();
            String href = el.attr("href");
            entry.put("tag", el.tagName().toLowerCase(Locale.ROOT));
            entry.put("href", href);
            entry.put("text", el.text().trim());
            String rel = el.attr("rel");
            if (!rel.isEmpty()) {
                entry.put("rel", rel);
            }
            String target = el.attr("target");
            if (!target.isEmpty()) {
                entry.put("target", target);
            }
            entry.put("kind", classifyLink(href));
            sourceMapBuilder.put("$.document.links[" + index + "]", rangeFor(el), precisionFor(el));
            index++;
        }
    }

    private String classifyLink(String href) {
        if (href.isEmpty()) {
            return "empty";
        }
        if (href.startsWith("#")) {
            return "fragment";
        }
        if (href.startsWith("javascript:")) {
            return "javascript";
        }
        if (href.startsWith("mailto:")) {
            return "mailto";
        }
        if (href.startsWith("https://") || href.startsWith("http://")) {
            return "external";
        }
        return "relative";
    }

    private void collectTables(Element root, ArrayNode tables, HtmlSourceMapBuilder sourceMapBuilder) {
        int index = 0;
        for (Element table : root.select("table")) {
            ObjectNode entry = tables.addObject();
            ArrayNode headers = entry.putArray("headers");
            List<String> headerNames = extractHeaders(table);
            for (String h : headerNames) {
                headers.add(h);
            }
            ArrayNode rows = entry.putArray("rows");
            for (Map<String, String> row : extractRows(table, headerNames)) {
                ObjectNode rowNode = rows.addObject();
                for (Map.Entry<String, String> cell : row.entrySet()) {
                    rowNode.put(cell.getKey(), cell.getValue());
                }
            }
            sourceMapBuilder.put("$.document.tables[" + index + "]", rangeFor(table), precisionFor(table));
            index++;
        }
    }

    private List<String> extractHeaders(Element table) {
        List<String> headers = new ArrayList<>();
        org.jsoup.select.Elements headerCells = table.select("thead tr:first-child th");
        if (headerCells.isEmpty()) {
            headerCells = table.select("tr:first-child th");
        }
        for (Element cell : headerCells) {
            headers.add(cell.text().trim());
        }
        return headers;
    }

    private List<Map<String, String>> extractRows(Element table, List<String> headers) {
        List<Map<String, String>> rows = new ArrayList<>();
        if (headers.isEmpty()) {
            return rows;
        }
        for (Element rowEl : table.select("tr")) {
            if (!rowEl.select("th").isEmpty()) {
                continue;
            }
            org.jsoup.select.Elements cells = rowEl.select("td");
            if (cells.isEmpty()) {
                continue;
            }
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String value = i < cells.size() ? cells.get(i).text().trim() : "";
                rowMap.put(headers.get(i), value);
            }
            rows.add(rowMap);
        }
        return rows;
    }

    private void collectForms(Element root, ArrayNode forms, HtmlSourceMapBuilder sourceMapBuilder) {
        int index = 0;
        for (Element form : root.select("form")) {
            ObjectNode entry = forms.addObject();
            entry.put("action", form.attr("action"));
            entry.put("method", form.attr("method").toLowerCase(Locale.ROOT));
            ArrayNode fields = entry.putArray("fields");
            for (Element field : form.select("input, select, textarea")) {
                ObjectNode f = fields.addObject();
                f.put("tag", field.tagName().toLowerCase(Locale.ROOT));
                String type = field.attr("type");
                if (!type.isEmpty()) {
                    f.put("type", type.toLowerCase(Locale.ROOT));
                }
                f.put("name", field.attr("name"));
                String idAttr = field.id();
                if (!idAttr.isEmpty()) {
                    f.put("id", idAttr);
                }
                boolean labeled = !idAttr.isEmpty()
                        && !root.select("label[for=" + idAttr + "]").isEmpty();
                f.put("labeled", labeled);
            }
            sourceMapBuilder.put("$.document.forms[" + index + "]", rangeFor(form), precisionFor(form));
            index++;
        }
    }

    private void collectScripts(Element root, ArrayNode scripts, HtmlSourceMapBuilder sourceMapBuilder) {
        int index = 0;
        for (Element script : root.select("script")) {
            ObjectNode entry = scripts.addObject();
            String src = script.attr("src");
            entry.put("src", src);
            entry.put("inline", src.isEmpty());
            String type = script.attr("type");
            if (!type.isEmpty()) {
                entry.put("type", type);
            }
            sourceMapBuilder.put("$.document.scripts[" + index + "]", rangeFor(script), precisionFor(script));
            index++;
        }
    }

    private String precisionFor(Element element) {
        return element.sourceRange().isTracked()
                ? HtmlSourceMapBuilder.PRECISION_EXACT
                : HtmlSourceMapBuilder.PRECISION_APPROXIMATE;
    }

    private SourceRange rangeFor(Element element) {
        org.jsoup.nodes.Range range = element.sourceRange();
        if (range.isTracked()) {
            int startLine = range.start().lineNumber();
            int startCol = range.start().columnNumber();
            int endLine = range.end().lineNumber();
            int endCol = range.end().columnNumber();
            return new SourceRange(startLine, startCol, endLine, endCol);
        }
        return new SourceRange(1, 1, 1, 1);
    }
}
