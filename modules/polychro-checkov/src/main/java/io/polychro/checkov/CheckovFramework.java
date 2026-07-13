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
package io.polychro.checkov;

/**
 * Checkov framework identifiers used for the --framework flag.
 */
enum CheckovFramework {
    TERRAFORM("terraform"),
    KUBERNETES("kubernetes"),
    CLOUDFORMATION("cloudformation"),
    DOCKERFILE("dockerfile"),
    YAML("yaml");

    private final String value;

    CheckovFramework(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}
