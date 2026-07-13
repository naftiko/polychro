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
package io.polychro.format.common;

import java.util.Locale;

/**
 * Format-agnostic link classification.
 *
 * <p>The resolver normalises {@code target} into a {@link LinkKind} and splits its file and
 * fragment components so downstream rules can reason about anchor resolution and on-disk
 * existence without re-implementing URL parsing per format.
 */
public final class LinkResolver {

    private LinkResolver() {
    }

    /**
     * Classify {@code rawTarget} and return the resulting {@link LinkReference}.
     *
     * <p><strong>Query and fragment handling.</strong> When a relative target carries a fragment,
     * the fragment is captured and the query (if any) is stripped from the file part, so
     * {@code "docs/a.md?v=1#tail"} resolves to {@link LinkKind#RELATIVE_FILE} with file part
     * {@code "docs/a.md"} and fragment {@code "tail"}. When the same shape appears <em>without</em> a
     * path — {@code "?q=1#tail"} — the fragment keeps the reference live and the result is still
     * {@link LinkKind#RELATIVE_FILE} (empty file part, fragment {@code "tail"}). However, a
     * fragment-less query-only target such as {@code "?just-a-query"} carries no resolvable file
     * <em>and</em> no anchor, so it is intentionally classified {@link LinkKind#MALFORMED}. This
     * asymmetry is by design: a fragment is a resolvable reference target, a bare query is not.
     *
     * @param rawTarget the raw link target as written in the source, may be {@code null}
     * @param path      the JsonPath of the projected node carrying the reference, never
     *                  {@code null}
     * @return the classified reference, never {@code null}
     */
    public static LinkReference resolve(String rawTarget, String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        String target = rawTarget == null ? "" : rawTarget;
        String trimmed = target.trim();
        if (trimmed.isEmpty()) {
            return new LinkReference(target, LinkKind.EMPTY, path, null, "");
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:")) {
            return new LinkReference(target, LinkKind.JAVASCRIPT, path, null, "");
        }
        if (lower.startsWith("data:")) {
            return new LinkReference(target, LinkKind.DATA, path, null, "");
        }
        if (lower.startsWith("mailto:")) {
            return new LinkReference(target, LinkKind.MAILTO, path, null, "");
        }
        if (lower.startsWith("tel:")) {
            return new LinkReference(target, LinkKind.TEL, path, null, "");
        }
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")) {
            return new LinkReference(target, LinkKind.EXTERNAL, path, null, "");
        }

        if (trimmed.startsWith("#")) {
            String fragment = trimmed.substring(1);
            return new LinkReference(target, LinkKind.INTERNAL_ANCHOR, path,
                    fragment.isEmpty() ? null : fragment, "");
        }

        // Relative file (with optional fragment + optional query)
        int fragmentIdx = trimmed.indexOf('#');
        String fragment = null;
        String pathPart = trimmed;
        if (fragmentIdx >= 0) {
            fragment = trimmed.substring(fragmentIdx + 1);
            pathPart = trimmed.substring(0, fragmentIdx);
            if (fragment.isEmpty()) {
                fragment = null;
            }
        }
        int queryIdx = pathPart.indexOf('?');
        if (queryIdx >= 0) {
            pathPart = pathPart.substring(0, queryIdx);
        }
        if (pathPart.isEmpty() && fragment == null) {
            return new LinkReference(target, LinkKind.MALFORMED, path, null, "");
        }
        return new LinkReference(target, LinkKind.RELATIVE_FILE, path, fragment, pathPart);
    }
}
