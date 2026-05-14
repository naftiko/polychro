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

class ExternalLinkCheckerTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        server.createContext("/ok", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        server.createContext("/not-found", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        server.createContext("/server-error", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        server.createContext("/head-not-allowed", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            } else {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            }
        });

        server.createContext("/head-not-allowed-get-fails", exchange -> {
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
            } else {
                exchange.sendResponseHeaders(503, -1);
            }
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
    void checkShouldReturnNoDiagnosticFor200() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/ok", 1));

        List<Diagnostic> diagnostics = checker.check(links);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldReportWarnFor404() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/not-found", 3));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
        assertEquals("broken-external-link", diagnostics.get(0).code());
        assertTrue(diagnostics.get(0).message().contains("404"));
    }

    @Test
    void checkShouldReportWarnFor500() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/server-error", 2));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
        assertTrue(diagnostics.get(0).message().contains("500"));
    }

    @Test
    void checkShouldReportWarnForDnsFailure() {
        ExternalLinkChecker checker = new ExternalLinkChecker(2000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("http://this-domain-definitely-does-not-exist-xyzzy.invalid/page", 1));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
        assertEquals("broken-external-link", diagnostics.get(0).code());
    }

    @Test
    void checkShouldReportWarnForConnectionRefused() {
        ExternalLinkChecker checker = new ExternalLinkChecker(2000, 100);
        // Use a port that is very unlikely to be open
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("http://127.0.0.1:1/unreachable", 1));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
    }

    @Test
    void checkShouldFallbackToGetOn405Success() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/head-not-allowed", 1));

        List<Diagnostic> diagnostics = checker.check(links);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    void checkShouldReportWarnWhenFallbackGetFails() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo(baseUrl + "/head-not-allowed-get-fails", 1));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
        assertTrue(diagnostics.get(0).message().contains("503"));
    }

    @Test
    void checkShouldReportWarnForInvalidUrl() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 100);
        List<MarkdownValidator.LinkInfo> links = List.of(
                new MarkdownValidator.LinkInfo("http://[invalid", 1));

        List<Diagnostic> diagnostics = checker.check(links);
        assertEquals(1, diagnostics.size());
        assertEquals(Severity.WARN, diagnostics.get(0).severity());
        assertEquals("broken-external-link", diagnostics.get(0).code());
    }

    @Test
    void checkUrlShouldThrottleRepeatedRequests() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 5);

        assertNull(checker.checkUrl(baseUrl + "/ok?first", 1));

        long start = System.nanoTime();
        assertNull(checker.checkUrl(baseUrl + "/ok?second", 2));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMs >= 150, "Expected throttled request to wait, elapsed=" + elapsedMs + "ms");
    }

    @Test
    void checkUrlShouldPreserveInterruptWhenThrottleSleepIsInterrupted() {
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 1);

        assertNull(checker.checkUrl(baseUrl + "/ok?first", 1));

        Thread.currentThread().interrupt();
        try {
            Diagnostic diagnostic = checker.checkUrl(baseUrl + "/ok?second", 2);

            assertNotNull(diagnostic);
            assertEquals(Severity.INFO, diagnostic.severity());
            assertEquals("unreachable-external-link", diagnostic.code());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
