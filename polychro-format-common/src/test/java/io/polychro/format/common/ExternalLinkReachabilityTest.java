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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ExternalLinkReachabilityTest {

    @Test
    void disabledProbeShortCircuitsWithoutCallingTransport() {
        AtomicInteger calls = new AtomicInteger();
        ExternalLinkProbe probe = new ExternalLinkProbe(false, url -> {
            calls.incrementAndGet();
            return ProbeResult.ok(url, 200);
        });
        assertFalse(probe.isEnabled());
        ProbeResult result = probe.probe("https://example.com/a");
        assertEquals(ProbeResult.Status.DISABLED, result.status());
        assertEquals(-1, result.statusCode());
        assertEquals(0, calls.get());
    }

    @Test
    void enabledProbeReturnsOk() {
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> ProbeResult.ok(url, 200));
        ProbeResult result = probe.probe("https://example.com/a");
        assertEquals(ProbeResult.Status.OK, result.status());
        assertEquals(200, result.statusCode());
        assertTrue(probe.isEnabled());
    }

    @Test
    void enabledProbeReturnsRedirect() {
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> ProbeResult.redirect(url, 301));
        ProbeResult result = probe.probe("https://example.com/old");
        assertEquals(ProbeResult.Status.REDIRECT, result.status());
        assertEquals(301, result.statusCode());
    }

    @Test
    void enabledProbeReturnsNotFound() {
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> ProbeResult.notFound(url, 404));
        ProbeResult result = probe.probe("https://example.com/missing");
        assertEquals(ProbeResult.Status.NOT_FOUND, result.status());
        assertEquals(404, result.statusCode());
    }

    @Test
    void enabledProbeReturnsTimeout() {
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> ProbeResult.timeout(url, "deadline"));
        ProbeResult result = probe.probe("https://example.com/slow");
        assertEquals(ProbeResult.Status.TIMEOUT, result.status());
        assertEquals("deadline", result.error());
    }

    @Test
    void enabledProbeReturnsError() {
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> ProbeResult.error(url, "dns"));
        ProbeResult result = probe.probe("https://nowhere.invalid");
        assertEquals(ProbeResult.Status.ERROR, result.status());
        assertEquals("dns", result.error());
    }

    @Test
    void resultsAreCachedAcrossCalls() {
        AtomicInteger calls = new AtomicInteger();
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> {
            calls.incrementAndGet();
            return ProbeResult.ok(url, 200);
        });
        probe.probe("https://example.com/a");
        probe.probe("https://example.com/a");
        probe.probe("https://example.com/b");
        assertEquals(2, calls.get());
    }

    @Test
    void constructorRejectsNullTransport() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalLinkProbe(true, null));
    }

    @Test
    void probeRejectsNullUrl() {
        ExternalLinkProbe probe = new ExternalLinkProbe(true, url -> ProbeResult.ok(url, 200));
        assertThrows(IllegalArgumentException.class, () -> probe.probe(null));
    }
}
