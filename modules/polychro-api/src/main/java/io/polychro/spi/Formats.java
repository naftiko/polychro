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
package io.polychro.spi;

import java.util.Locale;

/**
 * Shared utilities for canonicalising document format identifiers.
 * <p>
 * Both {@link Document} and validators (e.g. ruleset format filtering) must agree on which
 * spellings of {@code "yml"}, {@code "md"}, {@code "htm"} map to which canonical format. This
 * class is the single source of truth for that mapping — extending the alias set here automatically
 * propagates to every caller.
 */
public final class Formats {

    private Formats() {
        // utility class
    }

    /**
     * Normalise the given format identifier to its canonical form.
     * <p>
     * Recognised aliases:
     * <ul>
     *     <li>{@code "yml"} → {@code "yaml"}</li>
     *     <li>{@code "md"} → {@code "markdown"}</li>
     *     <li>{@code "htm"} → {@code "html"}</li>
     * </ul>
     * Any other value is lower-cased and returned as-is. {@code null} or blank input returns
     * {@code null}.
     *
     * @param format the format identifier to normalise, may be {@code null}
     * @return the canonical format identifier, or {@code null} when input is {@code null} or blank
     */
    public static String normalize(String format) {
        if (format == null || format.isBlank()) {
            return null;
        }
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "yml" -> "yaml";
            case "md" -> "markdown";
            case "htm" -> "html";
            default -> format.toLowerCase(Locale.ROOT);
        };
    }
}
