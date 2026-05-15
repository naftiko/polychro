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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkResolverTest {

    private static final String PATH = "$.x";

    @Test
    void shouldClassifyEmptyAndWhitespace() {
        assertEquals(LinkKind.EMPTY, LinkResolver.resolve(null, PATH).kind());
        assertEquals(LinkKind.EMPTY, LinkResolver.resolve("", PATH).kind());
        assertEquals(LinkKind.EMPTY, LinkResolver.resolve("   ", PATH).kind());
    }

    @Test
    void shouldClassifyAbsoluteHttp() {
        LinkReference http = LinkResolver.resolve("http://example.com/x", PATH);
        assertEquals(LinkKind.EXTERNAL, http.kind());

        LinkReference https = LinkResolver.resolve("HTTPS://EXAMPLE.COM", PATH);
        assertEquals(LinkKind.EXTERNAL, https.kind());

        LinkReference protocolRel = LinkResolver.resolve("//cdn.example.com/lib.js", PATH);
        assertEquals(LinkKind.EXTERNAL, protocolRel.kind());
    }

    @Test
    void shouldClassifySchemeSpecificUris() {
        assertEquals(LinkKind.JAVASCRIPT, LinkResolver.resolve("javascript:void(0)", PATH).kind());
        assertEquals(LinkKind.DATA, LinkResolver.resolve("data:text/plain;base64,XYZ", PATH).kind());
        assertEquals(LinkKind.MAILTO, LinkResolver.resolve("mailto:a@b.com", PATH).kind());
        assertEquals(LinkKind.TEL, LinkResolver.resolve("tel:+1-555-0100", PATH).kind());
    }

    @Test
    void shouldClassifyInternalAnchorWithAndWithoutFragment() {
        LinkReference withFragment = LinkResolver.resolve("#section-1", PATH);
        assertEquals(LinkKind.INTERNAL_ANCHOR, withFragment.kind());
        assertEquals("section-1", withFragment.fragment());

        LinkReference bareHash = LinkResolver.resolve("#", PATH);
        assertEquals(LinkKind.INTERNAL_ANCHOR, bareHash.kind());
        assertNull(bareHash.fragment());
    }

    @Test
    void shouldClassifyRelativeFile() {
        LinkReference simple = LinkResolver.resolve("../sibling.md", PATH);
        assertEquals(LinkKind.RELATIVE_FILE, simple.kind());
        assertEquals("../sibling.md", simple.filePart());
        assertNull(simple.fragment());
    }

    @Test
    void shouldStripFragmentAndQueryFromRelativeFile() {
        LinkReference withFragment = LinkResolver.resolve("docs/a.md#intro", PATH);
        assertEquals(LinkKind.RELATIVE_FILE, withFragment.kind());
        assertEquals("docs/a.md", withFragment.filePart());
        assertEquals("intro", withFragment.fragment());

        LinkReference withQuery = LinkResolver.resolve("docs/a.md?v=1", PATH);
        assertEquals(LinkKind.RELATIVE_FILE, withQuery.kind());
        assertEquals("docs/a.md", withQuery.filePart());

        LinkReference withBoth = LinkResolver.resolve("a.md?v=1#tail", PATH);
        assertEquals("a.md", withBoth.filePart());
        assertEquals("tail", withBoth.fragment());

        LinkReference emptyFragment = LinkResolver.resolve("a.md#", PATH);
        assertEquals("a.md", emptyFragment.filePart());
        assertNull(emptyFragment.fragment());
    }

    @Test
    void shouldClassifyMalformedTarget() {
        // ?-only target after stripping fragment-less query and empty path
        LinkReference malformed = LinkResolver.resolve("?just-a-query", PATH);
        assertEquals(LinkKind.MALFORMED, malformed.kind());
    }

    @Test
    void shouldClassifyEmptyPathWithFragmentAsRelativeFile() {
        // "?query#fragment" leaves pathPart empty after query-stripping but fragment is set.
        LinkReference result = LinkResolver.resolve("?q=1#tail", PATH);
        assertEquals(LinkKind.RELATIVE_FILE, result.kind());
        assertEquals("", result.filePart());
        assertEquals("tail", result.fragment());
    }

    @Test
    void shouldRejectNullPath() {
        assertThrows(IllegalArgumentException.class, () -> LinkResolver.resolve("x", null));
    }
}
