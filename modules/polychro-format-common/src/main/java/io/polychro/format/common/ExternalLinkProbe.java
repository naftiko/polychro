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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Cached HTTP reachability probe for external links.
 *
 * <p>The probe is opt-in: when constructed disabled, every call to {@link #probe(String)} returns
 * {@link ProbeResult.Status#DISABLED} immediately without invoking the underlying transport. When
 * enabled, results are memoised in an in-memory cache keyed by URL so successive checks across
 * documents share work.
 *
 * <p>The actual HTTP call is injected as a {@link Function} to keep the module dependency-free
 * and trivially testable. Production callers wire {@link java.net.http.HttpClient} into the
 * function; tests inject deterministic stubs.
 */
public final class ExternalLinkProbe {

    private final boolean enabled;
    private final Function<String, ProbeResult> transport;
    private final ConcurrentMap<String, ProbeResult> cache;

    /**
     * Create a new probe.
     *
     * @param enabled   when {@code false}, every call to {@link #probe(String)} short-circuits
     *                  with {@link ProbeResult.Status#DISABLED}
     * @param transport the HTTP transport function; only invoked when {@code enabled} is
     *                  {@code true} and the URL is not cached. Must not be {@code null}.
     */
    public ExternalLinkProbe(boolean enabled, Function<String, ProbeResult> transport) {
        if (transport == null) {
            throw new IllegalArgumentException("transport must not be null");
        }
        this.enabled = enabled;
        this.transport = transport;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Probe {@code url} for reachability.
     *
     * @param url the URL to probe; must be non-{@code null}
     * @return the probe outcome; cached after the first non-disabled invocation
     */
    public ProbeResult probe(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        if (!enabled) {
            return ProbeResult.disabled(url);
        }
        return cache.computeIfAbsent(url, transport);
    }

    /**
     * @return whether this probe is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
