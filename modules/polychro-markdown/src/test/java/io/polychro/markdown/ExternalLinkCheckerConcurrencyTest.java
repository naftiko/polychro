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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code polychro serve} builds one {@code Linter}, so every request shares one
 * {@link MarkdownValidator} and its {@link ExternalLinkChecker} (#211). These tests call one
 * checker from several threads at once.
 */
class ExternalLinkCheckerConcurrencyTest {

    private HttpServer server;
    private String baseUrl;
    private final List<Long> arrivalsNanos = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger hits = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        // Several handler threads, so the server itself does not space requests out.
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/ok", exchange -> {
            arrivalsNanos.add(System.nanoTime());
            hits.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/not-found", exchange -> {
            hits.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        ((ExecutorService) server.getExecutor()).shutdownNow();
    }

    @Test
    void checkUrlShouldSpaceConcurrentRequestsByTheRateLimit() throws Exception {
        // 4 requests per second: one request every 250 ms across all threads, not per thread.
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 4);
        int threads = 4;
        // Warm-up: the first request also creates the HTTP client and opens the connection,
        // which would delay its arrival and shrink the gap measured after it.
        assertNull(checker.checkUrl(baseUrl + "/ok?warm-up", 0));
        arrivalsNanos.clear();

        runTogether(threads, i -> assertNull(checker.checkUrl(baseUrl + "/ok?n=" + i, i)));

        List<Long> sorted = new ArrayList<>(arrivalsNanos);
        Collections.sort(sorted);
        assertEquals(threads, sorted.size());
        for (int i = 1; i < sorted.size(); i++) {
            long gapMs = (sorted.get(i) - sorted.get(i - 1)) / 1_000_000;
            // 250 ms apart, with some slack for scheduling.
            assertTrue(gapMs >= 200,
                    "Requests " + (i - 1) + " and " + i + " were only " + gapMs + " ms apart");
        }
    }

    @Test
    void checkUrlShouldCacheEveryResultWhenCalledFromManyThreads() throws Exception {
        // No rate limit, so the threads really write to the caches at the same time.
        ExternalLinkChecker checker = new ExternalLinkChecker(5000, 0);
        int threads = 8;
        int urlsPerThread = 250;

        runTogether(threads, t -> {
            for (int i = 0; i < urlsPerThread; i++) {
                String path = i % 2 == 0 ? "/ok" : "/not-found";
                checker.checkUrl(baseUrl + path + "?t=" + t + "&i=" + i, i);
            }
        });

        assertEquals(threads * urlsPerThread, hits.get());
        assertEquals(threads * urlsPerThread, checker.cacheSize(),
                "every checked URL should be in the success or the failure cache");

        // Every result is cached, so checking the same URLs again sends no request.
        runTogether(threads, t -> {
            for (int i = 0; i < urlsPerThread; i++) {
                String path = i % 2 == 0 ? "/ok" : "/not-found";
                checker.checkUrl(baseUrl + path + "?t=" + t + "&i=" + i, i);
            }
        });
        assertEquals(threads * urlsPerThread, hits.get());
    }

    /** Runs {@code task} once per thread, releasing all threads at the same moment. */
    private static void runTogether(int threads, IntTask task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int id = t;
                futures.add(pool.submit(() -> {
                    start.await();
                    task.run(id);
                    return null;
                }));
            }
            start.countDown();
            assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
                for (Future<?> future : futures) {
                    future.get();
                }
            });
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface IntTask {
        void run(int id) throws Exception;
    }
}
