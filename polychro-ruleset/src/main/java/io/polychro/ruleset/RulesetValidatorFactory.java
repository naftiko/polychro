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
package io.polychro.ruleset;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.nio.file.Path;

/**
 * Factory for creating {@link RulesetValidator} instances.
 * <p>
 * Discovered via {@link java.util.ServiceLoader} registration.
 * Configuration accepts:
 * <ul>
 *   <li>{@code rulesetPath} — file path to the Spectral-format YAML ruleset</li>
 *   <li>{@code rulesetContent} — inline YAML content (alternative to path)</li>
 *   <li>{@code includeNonRecommended} — whether to enable non-recommended rules (default: false)</li>
 * </ul>
 */
public class RulesetValidatorFactory implements ValidatorFactory {

    @Override
    public String name() {
        return "ruleset";
    }

    @Override
    public Validator create(ValidatorConfig config) {
        Ruleset ruleset = loadRuleset(config);
        boolean includeNonRecommended = config.get("includeNonRecommended", Boolean.class)
                .orElse(false);
        return new RulesetValidator(ruleset, includeNonRecommended);
    }

    private Ruleset loadRuleset(ValidatorConfig config) {
        RulesetParser parser = new RulesetParser();
        RulesetComposer composer = new RulesetComposer(parser);

        var pathOpt = config.get("rulesetPath", String.class);
        if (pathOpt.isPresent()) {
            Path rulesetPath = Path.of(pathOpt.get());
            Ruleset ruleset = parser.parse(rulesetPath);
            Path baseDir = rulesetPath.toAbsolutePath().getParent();
            return composer.compose(ruleset, baseDir, rulesetPath.normalize().toString());
        }

        var contentOpt = config.get("rulesetContent", String.class);
        if (contentOpt.isPresent()) {
            Ruleset ruleset = parser.parse(contentOpt.get());
            return composer.compose(ruleset);
        }

        throw new RulesetParseException("No ruleset configured: provide either 'rulesetPath' or 'rulesetContent'");
    }
}
