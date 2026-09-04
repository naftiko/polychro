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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Content-based spec-level format detector, layered on top of {@link Document}'s syntax-level
 * {@link Document#format() format} (yaml/json/xml/...).
 *
 * <p>{@link Document#format()} only ever carries the <em>syntax</em> a document is written in; it
 * has no notion of the <em>specification</em> a structured (yaml/json) document declares itself to
 * be — OpenAPI v2 ("Swagger"), OpenAPI v3, or AsyncAPI. Spectral-format rulesets scope rules with
 * {@code formats: [oas2]} / {@code [oas3]} / {@code [aas2]} / {@code [aas3]}, which has no
 * counterpart in Polychro without this detector (naftiko/polychro#83, Priority 3 of #76).
 *
 * <p>Detection mirrors Spectral's own {@code @stoplight/spectral-formats} package
 * ({@code packages/formats/src/openapi.ts}, {@code asyncapi.ts}): a root object carrying a
 * {@code swagger} key whose value parses to major version {@code 2} is {@code oas2}; a root object
 * carrying an {@code openapi} key whose value parses to major version {@code 3} is {@code oas3}
 * (both via Spectral's lenient {@code parseInt}-style leading-digits match); a root object
 * carrying an {@code asyncapi} key is {@code aas2} or {@code aas3} only when that key's value is a
 * <strong>complete {@code major.minor.patch}</strong> semver string (e.g. {@code 2.6.0}) — Spectral
 * validates {@code asyncapi} with the strict regex {@code ^2\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$} /
 * {@code ^3\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$}, unlike its lenient {@code oas2}/{@code oas3}
 * leading-digits check, so {@code asyncapi: 2}, {@code asyncapi: 2.6}, or {@code asyncapi: 2junk}
 * are all "unknown" formats in Spectral and must stay unknown here too. Each of {@code oas2},
 * {@code oas3}, {@code aas2}, and {@code aas3} is detected independently from its own key — a
 * malformed or synthetic document that carries more than one of {@code swagger}, {@code openapi},
 * or {@code asyncapi} can therefore match more than one format simultaneously (mirroring
 * Spectral's own detectors, which make the same independent, non-exclusive checks); a real,
 * well-formed spec document declares exactly one of those keys and so matches exactly one format
 * in practice.
 */
public final class SpecFormats {

    /** Full {@code major.minor.patch} semver, major version 2 — mirrors Spectral's {@code aas2Regex}. */
    private static final Pattern AAS2_VERSION = Pattern.compile("^2\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");

    /** Full {@code major.minor.patch} semver, major version 3 — mirrors Spectral's {@code aas3Regex}. */
    private static final Pattern AAS3_VERSION = Pattern.compile("^3\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");

    private SpecFormats() {
        // utility class
    }

    /**
     * Detects the spec-level format(s) declared by a structured document root.
     *
     * <p>Each candidate format is an independent predicate, mirroring Spectral's own
     * {@code @stoplight/spectral-formats} detectors: a document carrying both a {@code swagger}
     * key parsing to major version 2 <em>and</em> an {@code openapi} key parsing to major version
     * 3 matches both {@code oas2} and {@code oas3}. Real-world documents only ever declare one of
     * {@code swagger}/{@code openapi}/{@code asyncapi}, so this rarely matters in practice, but the
     * detector must not assume it.
     *
     * @param root the parsed document root; may be {@code null} or non-object, in which case no
     *             spec format is detected
     * @return the detected spec format identifiers ({@code "oas2"}, {@code "oas3"}, {@code "aas2"},
     *         {@code "aas3"}), or an empty list when none is recognized
     */
    public static List<String> detect(JsonNode root) {
        if (root == null || !root.isObject()) {
            return List.of();
        }

        List<String> formats = new ArrayList<>();

        Integer swaggerMajor = majorVersion(root.get("swagger"));
        if (swaggerMajor != null && swaggerMajor == 2) {
            formats.add("oas2");
        }

        Integer openapiMajor = majorVersion(root.get("openapi"));
        if (openapiMajor != null && openapiMajor == 3) {
            formats.add("oas3");
        }

        String asyncapiVersion = versionText(root.get("asyncapi"));
        if (asyncapiVersion != null) {
            if (AAS2_VERSION.matcher(asyncapiVersion).matches()) {
                formats.add("aas2");
            }
            if (AAS3_VERSION.matcher(asyncapiVersion).matches()) {
                formats.add("aas3");
            }
        }

        return List.copyOf(formats);
    }

    /**
     * Parses the leading major-version integer out of a version-like node (e.g. {@code "3.0.2"} or
     * {@code 2.0}), mirroring Spectral's own lenient {@code parseInt}/{@code Number.parseInt}
     * detection used for {@code oas2}/{@code oas3}. Returns {@code null} when the node is absent or
     * its text does not start with a recognizable integer.
     */
    private static Integer majorVersion(JsonNode versionNode) {
        if (versionNode == null || versionNode.isNull()) {
            return null;
        }
        String trimmed = versionNode.asText("").strip();
        int end = 0;
        while (end < trimmed.length() && Character.isDigit(trimmed.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed.substring(0, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return the text of {@code versionNode} verbatim, or {@code null} when the node is absent
     *         or JSON null — used for the strict {@code asyncapi} semver match, which (unlike
     *         {@link #majorVersion}) mirrors Spectral's own {@code String(asyncapi)} coercion:
     *         Spectral applies its anchored {@code aas2Regex}/{@code aas3Regex} directly to that
     *         string <strong>without trimming</strong>, so a padded value such as
     *         {@code " 2.6.0 "} does not match either regex and must stay unrecognized here too.
     */
    private static String versionText(JsonNode versionNode) {
        if (versionNode == null || versionNode.isNull()) {
            return null;
        }
        return versionNode.asText("");
    }

    /**
     * @return {@code true} when {@code candidate} is a recognized spec-level format identifier
     *         (case-insensitive)
     */
    public static boolean isSpecFormat(String candidate) {
        if (candidate == null) {
            return false;
        }
        String normalized = candidate.toLowerCase(Locale.ROOT);
        return "oas2".equals(normalized) || "oas3".equals(normalized)
                || "aas2".equals(normalized) || "aas3".equals(normalized);
    }
}
