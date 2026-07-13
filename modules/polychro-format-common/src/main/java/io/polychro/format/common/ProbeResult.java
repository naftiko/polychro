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

/**
 * Outcome of an external-link reachability probe.
 *
 * @param url        the probed URL
 * @param status     classification of the probe result
 * @param statusCode HTTP status code when {@code status} is {@link Status#OK} or
 *                   {@link Status#REDIRECT}, otherwise {@code -1}
 * @param error      diagnostic message when {@code status} is {@link Status#ERROR} or
 *                   {@link Status#TIMEOUT}, otherwise {@code null}
 */
public record ProbeResult(String url, ProbeResult.Status status, int statusCode, String error) {

    public enum Status {
        OK,
        REDIRECT,
        NOT_FOUND,
        TIMEOUT,
        ERROR,
        DISABLED
    }

    public static ProbeResult ok(String url, int statusCode) {
        return new ProbeResult(url, Status.OK, statusCode, null);
    }

    public static ProbeResult redirect(String url, int statusCode) {
        return new ProbeResult(url, Status.REDIRECT, statusCode, null);
    }

    public static ProbeResult notFound(String url, int statusCode) {
        return new ProbeResult(url, Status.NOT_FOUND, statusCode, null);
    }

    public static ProbeResult timeout(String url, String error) {
        return new ProbeResult(url, Status.TIMEOUT, -1, error);
    }

    public static ProbeResult error(String url, String error) {
        return new ProbeResult(url, Status.ERROR, -1, error);
    }

    public static ProbeResult disabled(String url) {
        return new ProbeResult(url, Status.DISABLED, -1, null);
    }
}
