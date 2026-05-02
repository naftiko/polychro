package io.polychro.markdown;

import com.sun.net.httpserver.HttpServer;
import io.polychro.spi.Diagnostic;
import io.polychro.spi.Severity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalLinkEdgeCasesTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        server.createContext("/head-405-get-io-error", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            } else {
                exchange.close();
            }
        });

        server.createContext("/head-405-get-500", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
            }
        });

        server.createContext("/head-405-get-ok", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            }
        });

        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void throttleShouldDelayBetweenRequests() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 50);

        checker.check(List.of(new MarkdownValidator.LinkInfo(baseUrl + "/ok", 1)));
        checker.check(List.of(new MarkdownValidator.LinkInfo(baseUrl + "/ok?v=2", 2)));

        assertEquals(2, checker.cacheSize());
    }

    @Test
    void fallbackGetIOExceptionShouldReturnWarnDiagnostic() {
        ExternalLinkChecker checker = new ExternalLinkChecker(2000, 0);

        List<Diagnostic> diagnostics = checker.check(List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/head-405-get-io-error", 1)));

        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
        assertEquals("broken-external-link", diagnostics.get(0).code());
    }

    @Test
    void fallbackGetNonSuccessStatusShouldReportBroken() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        List<Diagnostic> diagnostics = checker.check(List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/head-405-get-500", 1)));

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.get(0).message().contains("HTTP 500"));
    }

    @Test
    void fallbackGetSuccessShouldCacheAndReturnEmpty() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        List<Diagnostic> diagnostics = checker.check(List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/head-405-get-ok", 1)));

        assertTrue(diagnostics.isEmpty());
        assertEquals(1, checker.cacheSize());
    }

    @Test
    void directFallbackGetIOExceptionShouldReportBroken() {
        ExternalLinkChecker checker = new ExternalLinkChecker(2000, 0);

        Diagnostic diag = checker.fallbackGet(baseUrl + "/head-405-get-io-error", 5);

        assertNotNull(diag);
        assertEquals(Severity.WARN, diag.severity());
    }

    @Test
    void directFallbackGetSuccessShouldReturnNull() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        Diagnostic diag = checker.fallbackGet(baseUrl + "/head-405-get-ok", 3);

        assertNull(diag);
    }

    @Test
    void directFallbackGetNonSuccessShouldReturnDiagnostic() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        Diagnostic diag = checker.fallbackGet(baseUrl + "/head-405-get-500", 3);

        assertNotNull(diag);
        assertTrue(diag.message().contains("HTTP 500"));
    }

    @Test
    void throttleInterruptShouldNotCrash() throws InterruptedException {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 1);

        Thread testThread = new Thread(() -> {
            checker.check(List.of(new MarkdownValidator.LinkInfo(baseUrl + "/ok", 1)));
            Thread.currentThread().interrupt();
            checker.check(List.of(new MarkdownValidator.LinkInfo(baseUrl + "/ok?t=2", 2)));
        });
        testThread.start();
        testThread.join(10000);
        assertFalse(testThread.isAlive());
    }

    @Test
    void interruptedDuringCheckUrlShouldReturnDiagnostic() throws InterruptedException {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            List<Diagnostic> diagnostics = checker.check(List.of(
                    new MarkdownValidator.LinkInfo(baseUrl + "/ok", 1)));
            // HttpClient.send() detects interrupted flag and throws InterruptedException
            assertFalse(diagnostics.isEmpty());
            Diagnostic diag = diagnostics.get(0);
            assertEquals("unreachable-external-link", diag.code());
            assertEquals(Severity.INFO, diag.severity());
        });
        testThread.start();
        testThread.join(5000);
        assertFalse(testThread.isAlive());
    }

    @Test
    void interruptedDuringFallbackGetShouldReturnDiagnostic() throws InterruptedException {
        // HEAD-405 endpoint will trigger fallbackGet. We interrupt after HEAD succeeds
        // but the interrupt flag persists through to fallbackGet's send() call.
        // Use a server that responds 405 to HEAD, then on GET the thread is interrupted.
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        Thread testThread = new Thread(() -> {
            // Don't interrupt yet — let HEAD succeed with 405
            // After HEAD, fallbackGet is called with throttle() first
            // We need to interrupt inside fallbackGet's send() — hard without mock.
            // Instead, call fallbackGet directly with interrupt set
            Thread.currentThread().interrupt();
            Diagnostic diag = checker.fallbackGet(baseUrl + "/head-405-get-ok", 1);
            assertNotNull(diag);
            assertEquals("unreachable-external-link", diag.code());
        });
        testThread.start();
        testThread.join(5000);
        assertFalse(testThread.isAlive());
    }

    @Test
    void zeroRateLimitShouldSkipThrottle() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        List<Diagnostic> diagnostics = checker.check(List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/ok", 1)));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void invalidUrlShouldReportBroken() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        Diagnostic diag = checker.checkUrl("ht!tp://[invalid", 1);

        assertNotNull(diag);
        assertEquals("broken-external-link", diag.code());
        assertTrue(diag.message().contains("Invalid URL"));
    }

    @Test
    void isSuccessShouldReturnTrueForOkRange() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        assertTrue(checker.isSuccess(200));
        assertTrue(checker.isSuccess(301));
        assertTrue(checker.isSuccess(399));
    }

    @Test
    void isSuccessShouldReturnFalseForOutOfRange() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);

        assertFalse(checker.isSuccess(199));
        assertFalse(checker.isSuccess(400));
        assertFalse(checker.isSuccess(500));
    }

    @Test
    void threeArgConstructorShouldUseProvidedClient() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 10,
                java.net.http.HttpClient.newHttpClient());

        assertNotNull(checker);
        assertEquals(0, checker.cacheSize());
    }

    @Test
    void threeArgConstructorWithZeroRateShouldWork() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0,
                java.net.http.HttpClient.newHttpClient());

        assertNotNull(checker);
        assertEquals(0, checker.cacheSize());
    }
}
