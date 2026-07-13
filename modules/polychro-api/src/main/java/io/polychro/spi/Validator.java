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

import java.util.List;

/**
 * A document validator that produces diagnostics.
 * <p>
 * Implementations are discovered via {@link ValidatorFactory} and {@link java.util.ServiceLoader}.
 */
public interface Validator {

    /**
     * @return the unique name of this validator (e.g. "wellformedness", "json-schema", "ruleset")
     */
    String name();

    /**
     * Validate the given document and return any diagnostics found.
     *
     * @param doc the document to validate
     * @return a list of diagnostics, empty if the document is valid
     */
    List<Diagnostic> validate(Document doc);
}
