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

import java.util.List;

/**
 * Formats diagnostics into a specific output representation.
 */
public interface DiagnosticFormatter {

    /**
     * Format a list of diagnostics into a string.
     *
     * @param diagnostics the diagnostics to format
     * @return the formatted output string
     */
    String format(List<Diagnostic> diagnostics);
}
