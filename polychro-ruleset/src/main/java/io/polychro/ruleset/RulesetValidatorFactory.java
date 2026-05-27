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

import io.polychro.spi.Document;
import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.nio.file.Path;
import java.util.Set;

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

    /**
     * Formats this factory advertises as supported.
     *
     * <p>{@code json}, {@code yaml} and {@code xml} are <em>structured</em> formats:
     * documents are parsed into a Jackson tree and JSONPath {@code given} expressions
     * (e.g. {@code $.info.name}) are evaluated against that tree.
     *
     * <p>{@code markdown} and {@code html} are <em>text-node</em> formats today:
     * {@link Document#root()} is a {@code TextNode} holding the raw content, so a
     * JSONPath selector like {@code $.info.name} will never match and a ruleset
     * scoped to {@code formats: [markdown]} or {@code formats: [html]} will silently
     * produce zero diagnostics. They are listed here so callers can route documents
     * of those formats to the ruleset validator; the projection layer that exposes
     * structured AST nodes for these formats is introduced in PR #7 (markdown) and
     * PR #8 (html), at which point JSONPath rules become meaningful for them.
     */
    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "json", "yaml", "xml");

    @Override
    public String name() {
        return "ruleset";
    }

    @Override
    public Set<String> supportedFormats() {
        return SUPPORTED_FORMATS;
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

        // Use IllegalArgumentException (consistent with JsonSchemaValidatorFactory and
        // JsonStructureValidatorFactory) so the Linter.Builder can recognise this as
        // a "missing-config" signal and silently skip the factory when it was
        // auto-discovered without user-supplied configuration. See issue #20.
        throw new IllegalArgumentException(
                "RulesetValidatorFactory requires either 'rulesetPath' or 'rulesetContent' in config");
    }
}
