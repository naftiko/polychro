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
import io.polychro.spi.Severity;
import io.polychro.spi.SourceRange;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates external HTTP/HTTPS links in Markdown documents.
 * Performs HEAD requests with fallback to GET on 405, respects rate limiting and caching.
 *
 * <p>Safe to share across threads: {@code polychro serve} builds one validator for all requests,
 * so concurrent lints use the same checker. The caches are concurrent collections, and the rate
 * limit holds across all threads, not per thread.
 */
class ExternalLinkChecker {

    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    private static final int STATUS_OK_LOWER = 200;
    private static final int STATUS_OK_UPPER = 399;

    private final Duration timeout;
    private final long rateLimitDelayMs;
    private final Set<String> successCache;
    private final Map<String, Diagnostic> failureCache;
    /**
     * Created lazily by {@link #httpClient()} on the first check (an idle client's selector thread
     * delays JVM exit). Package-private so tests can read it directly.
     */
    HttpClient httpClient;
    /** Earliest time the next request may start. Guarded by {@link #throttleLock}. */
    private long nextRequestTimeMs;
    private final Object throttleLock = new Object();

    ExternalLinkChecker(int timeoutMs, int rateLimitPerSecond) {
        this.timeout = Duration.ofMillis(timeoutMs);
        this.rateLimitDelayMs = rateLimitPerSecond > 0 ? 1000L / rateLimitPerSecond : 0;
        this.successCache = ConcurrentHashMap.newKeySet();
        this.failureCache = new ConcurrentHashMap<>();
        this.nextRequestTimeMs = 0;
    }

    ExternalLinkChecker(int timeoutMs, int rateLimitPerSecond, HttpClient httpClient) {
        this.timeout = Duration.ofMillis(timeoutMs);
        this.rateLimitDelayMs = rateLimitPerSecond > 0 ? 1000L / rateLimitPerSecond : 0;
        this.successCache = ConcurrentHashMap.newKeySet();
        this.failureCache = new ConcurrentHashMap<>();
        this.httpClient = httpClient;
        this.nextRequestTimeMs = 0;
    }

    synchronized HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(timeout)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        return httpClient;
    }

    List<Diagnostic> check(List<MarkdownValidator.LinkInfo> links) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (MarkdownValidator.LinkInfo link : links) {
            String url = link.target();
            Diagnostic result = checkUrl(url, link.line());
            if (result != null) {
                diagnostics.add(result);
            }
        }

        return diagnostics;
    }

    Diagnostic checkUrl(String url, int line) {
        // Check cache first
        if (successCache.contains(url)) {
            return null;
        }
        if (failureCache.containsKey(url)) {
            Diagnostic cached = failureCache.get(url);
            return new Diagnostic(cached.severity(), cached.code(), cached.message(),
                    cached.path(), new SourceRange(line, 1, line, 1));
        }

        // Rate limiting
        throttle();

        try {
            URI uri = URI.create(url);
            HttpRequest headRequest = HttpRequest.newBuilder(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(timeout)
                    .build();

            HttpResponse<Void> response = httpClient().send(headRequest,
                    HttpResponse.BodyHandlers.discarding());

            int status = response.statusCode();

            if (status == STATUS_METHOD_NOT_ALLOWED) {
                // Fallback to GET with range header
                return fallbackGet(url, line);
            }

            if (isSuccess(status)) {
                successCache.add(url);
                return null;
            }

            Diagnostic diag = new Diagnostic(Severity.WARN, "broken-external-link",
                    "External link returned HTTP " + status + ": " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;

        } catch (IOException e) {
            Diagnostic diag = new Diagnostic(Severity.WARN, "broken-external-link",
                    "External link unreachable: " + url + " (" + e.getMessage() + ")",
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Not cached: an interrupted check says nothing about the link itself.
            return new Diagnostic(Severity.INFO, "unreachable-external-link",
                    "External link check interrupted: " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
        } catch (IllegalArgumentException e) {
            Diagnostic diag = new Diagnostic(Severity.WARN, "broken-external-link",
                    "Invalid URL: " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;
        }
    }

    Diagnostic fallbackGet(String url, int line) {
        throttle();

        try {
            URI uri = URI.create(url);
            HttpRequest getRequest = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("Range", "bytes=0-0")
                    .timeout(timeout)
                    .build();

            HttpResponse<Void> response = httpClient().send(getRequest,
                    HttpResponse.BodyHandlers.discarding());

            int status = response.statusCode();

            if (isSuccess(status)) {
                successCache.add(url);
                return null;
            }

            Diagnostic diag = new Diagnostic(Severity.WARN, "broken-external-link",
                    "External link returned HTTP " + status + ": " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;

        } catch (IOException e) {
            Diagnostic diag = new Diagnostic(Severity.WARN, "broken-external-link",
                    "External link unreachable: " + url + " (" + e.getMessage() + ")",
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Not cached: an interrupted check says nothing about the link itself.
            return new Diagnostic(Severity.INFO, "unreachable-external-link",
                    "External link check interrupted: " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
        }
    }

    boolean isSuccess(int status) {
        return status >= STATUS_OK_LOWER && status <= STATUS_OK_UPPER;
    }

    void clearCache() {
        successCache.clear();
        failureCache.clear();
    }

    int cacheSize() {
        return successCache.size() + failureCache.size();
    }

    /**
     * Waits for this request's turn. Each caller reserves the next free slot under the lock, then
     * sleeps outside it, so concurrent callers are spaced {@code rateLimitDelayMs} apart in total
     * without holding the lock while they wait.
     */
    private void throttle() {
        if (rateLimitDelayMs <= 0) {
            return;
        }
        long waitMs;
        synchronized (throttleLock) {
            long now = System.currentTimeMillis();
            long slot = Math.max(now, nextRequestTimeMs);
            nextRequestTimeMs = slot + rateLimitDelayMs;
            waitMs = slot - now;
        }
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
