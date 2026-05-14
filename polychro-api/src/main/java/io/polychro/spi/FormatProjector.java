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
 * Projects a native parsed format into Polychro's canonical {@link Document} model.
 *
 * @param <T> the native parsed representation consumed by the projector
 */
public interface FormatProjector<T> {

    /**
     * @return the canonical source format this projector accepts, such as {@code markdown} or {@code html}
     */
    String format();

    /**
     * Project the native parsed input into a canonical {@link Document}.
     *
     * @param parsed the native parsed representation
     * @param sourcePath the original source path or identifier, when available
     * @return the projected document
     */
    Document project(T parsed, String sourcePath);

    /**
     * Project the native parsed input without an associated source path.
     *
     * @param parsed the native parsed representation
     * @return the projected document
     */
    default Document project(T parsed) {
        return project(parsed, null);
    }
}