package io.polychro.markdown;

import com.sun.net.httpserver.HttpServer;
import io.polychro.spi.Diagnostic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalLinkCacheTest {

    private HttpServer server;
    private String baseUrl;
    private int requestCount;

    @BeforeEach
    void startServer() throws IOException {
        requestCount = 0;
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/counted", exchange -> {
            requestCount++;
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        server.createContext("/counted-fail", exchange -> {
            requestCount++;
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sameUrlShouldOnlyBeCheckedOnce() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        String url = baseUrl + "/counted";

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(url, 1),
                new MarkdownValidator.LinkInfo(url, 5));

        List<Diagnostic> diagnostics = checker.check(links);
        assertTrue(diagnostics.isEmpty());
        assertEquals(1, requestCount);
    }

    @Test
    void differentUrlsShouldBeCheckedSeparately() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);

        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/counted", 1),
                new MarkdownValidator.LinkInfo(baseUrl + "/counted-fail", 2));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size()); // only the failed URL
        assertEquals(2, requestCount);
    }

    @Test
    void clearCacheShouldAllowRecheck() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        String url = baseUrl + "/counted";

        checker.check(List.of(new MarkdownValidator.LinkInfo(url, 1)));
        assertEquals(1, requestCount);

        checker.clearCache();

        checker.check(List.of(new MarkdownValidator.LinkInfo(url, 1)));
        assertEquals(2, requestCount);
    }

    @Test
    void cacheSizeShouldReflectCheckedUrls() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        assertEquals(0, checker.cacheSize());

        checker.check(List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/counted", 1),
                new MarkdownValidator.LinkInfo(baseUrl + "/counted-fail", 2)));

        assertEquals(2, checker.cacheSize());
    }

    @Test
    void cachedFailureShouldReturnDiagnosticWithCorrectLine() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        String url = baseUrl + "/counted-fail";

        // First check at line 3
        List<Diagnostic> first = checker.check(List.of(new MarkdownValidator.LinkInfo(url, 3)));
        assertEquals(3, first.get(0).range().startLine());

        // Second check at line 10 — should use cache but adjust line
        List<Diagnostic> second = checker.check(List.of(new MarkdownValidator.LinkInfo(url, 10)));
        assertEquals(10, second.get(0).range().startLine());
        assertEquals(1, requestCount); // Only one request made
    }
}
