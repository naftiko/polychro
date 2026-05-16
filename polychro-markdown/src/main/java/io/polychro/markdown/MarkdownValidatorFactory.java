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
package io.polychro.markdown;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

import java.util.Set;

/**
 * Factory for creating {@link MarkdownValidator} instances.
 */
public class MarkdownValidatorFactory implements ValidatorFactory {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("markdown");
    private static final Set<String> SUPPORTED_PROFILES = Set.of(
            "generic", "skill", "agents", "instructions");

    @Override
    public String name() {
        return MarkdownValidator.NAME;
    }

    @Override
    public Set<String> supportedFormats() {
        return SUPPORTED_FORMATS;
    }

    @Override
    public Set<String> supportedProfiles() {
        return SUPPORTED_PROFILES;
    }

    @Override
    public Validator create(ValidatorConfig config) {
        int lineLength = config.get("lineLength", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_LINE_LENGTH);
        String listMarker = config.get("listMarker", String.class)
                .orElse(MarkdownValidator.DEFAULT_LIST_MARKER);
        String formatOverride = config.get("format", String.class).orElse(null);
        boolean checkExternalLinks = config.get("checkExternalLinks", Boolean.class)
                .orElse(false);
        int externalLinkTimeout = config.get("externalLinkTimeout", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_EXTERNAL_LINK_TIMEOUT_MS);
        int externalLinkRateLimit = config.get("externalLinkRateLimit", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_EXTERNAL_LINK_RATE_LIMIT);

        MarkdownFormat format;
        if (formatOverride != null) {
            format = FormatDetector.fromName(formatOverride);
        } else {
            // Format will be detected per-document; use generic as default
            format = new GenericFormat();
        }

        return new MarkdownValidator(lineLength, listMarker, format, new FrontmatterParser(),
                checkExternalLinks, externalLinkTimeout, externalLinkRateLimit);
    }

    /**
     * Create a validator that auto-detects format from the document's source path.
     */
    public Validator createWithAutoDetect(ValidatorConfig config, String sourcePath) {
        int lineLength = config.get("lineLength", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_LINE_LENGTH);
        String listMarker = config.get("listMarker", String.class)
                .orElse(MarkdownValidator.DEFAULT_LIST_MARKER);
        String formatOverride = config.get("format", String.class).orElse(null);
        boolean checkExternalLinks = config.get("checkExternalLinks", Boolean.class)
                .orElse(false);
        int externalLinkTimeout = config.get("externalLinkTimeout", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_EXTERNAL_LINK_TIMEOUT_MS);
        int externalLinkRateLimit = config.get("externalLinkRateLimit", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_EXTERNAL_LINK_RATE_LIMIT);

        MarkdownFormat format;
        if (formatOverride != null) {
            format = FormatDetector.fromName(formatOverride);
        } else {
            format = FormatDetector.detect(sourcePath);
        }

        return new MarkdownValidator(lineLength, listMarker, format, new FrontmatterParser(),
                checkExternalLinks, externalLinkTimeout, externalLinkRateLimit);
    }
}
