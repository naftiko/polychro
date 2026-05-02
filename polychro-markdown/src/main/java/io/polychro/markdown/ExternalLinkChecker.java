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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates external HTTP/HTTPS links in Markdown documents.
 * Performs HEAD requests with fallback to GET on 405, respects rate limiting and caching.
 */
class ExternalLinkChecker {

    private static final int STATUS_METHOD_NOT_ALLOWED = 405;
    private static final int STATUS_OK_LOWER = 200;
    private static final int STATUS_OK_UPPER = 399;

    private final Duration timeout;
    private final long rateLimitDelayMs;
    private final Set<String> successCache;
    private final Map<String, Diagnostic> failureCache;
    private final HttpClient httpClient;
    private long lastRequestTimeMs;

    ExternalLinkChecker(int timeoutMs, int rateLimitPerSecond) {
        this.timeout = Duration.ofMillis(timeoutMs);
        this.rateLimitDelayMs = rateLimitPerSecond > 0 ? 1000L / rateLimitPerSecond : 0;
        this.successCache = new HashSet<>();
        this.failureCache = new HashMap<>();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.lastRequestTimeMs = 0;
    }

    ExternalLinkChecker(int timeoutMs, int rateLimitPerSecond, HttpClient httpClient) {
        this.timeout = Duration.ofMillis(timeoutMs);
        this.rateLimitDelayMs = rateLimitPerSecond > 0 ? 1000L / rateLimitPerSecond : 0;
        this.successCache = new HashSet<>();
        this.failureCache = new HashMap<>();
        this.httpClient = httpClient;
        this.lastRequestTimeMs = 0;
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

            HttpResponse<Void> response = httpClient.send(headRequest,
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
            Diagnostic diag = new Diagnostic(Severity.INFO, "unreachable-external-link",
                    "External link check interrupted: " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;
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

            HttpResponse<Void> response = httpClient.send(getRequest,
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
            Diagnostic diag = new Diagnostic(Severity.INFO, "unreachable-external-link",
                    "External link check interrupted: " + url,
                    null,
                    new SourceRange(line, 1, line, 1));
            failureCache.put(url, diag);
            return diag;
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

    private void throttle() {
        if (rateLimitDelayMs <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTimeMs;
        if (elapsed < rateLimitDelayMs) {
            try {
                Thread.sleep(rateLimitDelayMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTimeMs = System.currentTimeMillis();
    }
}
