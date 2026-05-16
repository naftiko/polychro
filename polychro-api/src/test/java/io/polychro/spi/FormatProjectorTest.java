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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormatProjectorTest {

    @Test
    void projectShouldDefaultSourcePathToNull() {
        FormatProjector<String> projector = new FormatProjector<>() {
            @Override
            public String format() {
                return "markdown";
            }

            @Override
            public Document project(String parsed, String sourcePath) {
                return new Document(null, format(), sourcePath);
            }
        };

        Document document = projector.project("parsed-markdown");

        assertEquals("markdown", document.format());
        assertNull(document.sourcePath());
    }
}
