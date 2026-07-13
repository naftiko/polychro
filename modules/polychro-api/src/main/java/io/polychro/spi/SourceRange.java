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

/**
 * A 0-based line/column range within a source document.
 *
 * <p>The <strong>target convention</strong> is 0-based on both axes, to match Spectral and the LSP
 * {@code Position} convention that editor consumers expect (a value at file line 1 / column 1 is
 * {@code startLine == 0}, {@code startColumn == 0}). The end is exclusive.
 *
 * <p><strong>Known alpha limitation:</strong> only the YAML producer ({@link JacksonSourceMap}) is
 * fully aligned with this convention today. The Checkov, Markdown, HTML and JSON-Structure
 * producers still emit 1-based positions and are tracked as migration debt (see issues #34&ndash;#37).
 * Until those are migrated, do not assume every {@code SourceRange} in the engine is 0-based:
 * only ranges resolved from a YAML document are guaranteed to be.
 *
 * @param startLine   0-based start line
 * @param startColumn 0-based start column
 * @param endLine     0-based end line
 * @param endColumn   0-based exclusive end column
 */
public record SourceRange(int startLine, int startColumn, int endLine, int endColumn) {
}
