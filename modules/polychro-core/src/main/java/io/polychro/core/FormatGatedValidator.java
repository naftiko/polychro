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
package io.polychro.core;

import io.polychro.spi.Diagnostic;
import io.polychro.spi.Document;
import io.polychro.spi.Validator;

import java.util.List;
import java.util.Set;

/**
 * A {@link Validator} that delegates only when the document's
 * {@link Document#format() format} appears in a configured set of
 * supported formats. Used by {@link Linter.Builder} to ensure
 * format-specific validators (e.g. markdown, html, ruleset) do not
 * emit diagnostics on unrelated documents.
 *
 * @see io.polychro.spi.ValidatorFactory#supportedFormats()
 */
final class FormatGatedValidator implements Validator {

    private final Validator delegate;
    private final Set<String> supportedFormats;

    FormatGatedValidator(Validator delegate, Set<String> supportedFormats) {
        this.delegate = delegate;
        this.supportedFormats = supportedFormats;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public List<Diagnostic> validate(Document doc) {
        String format = doc == null ? null : doc.format();
        if (format == null || !supportedFormats.contains(format)) {
            return List.of();
        }
        return delegate.validate(doc);
    }

    /**
     * @return the wrapped validator (visible for tests).
     */
    Validator delegate() {
        return delegate;
    }

    /**
     * @return the formats this gate allows through (visible for tests).
     */
    Set<String> supportedFormats() {
        return supportedFormats;
    }
}
