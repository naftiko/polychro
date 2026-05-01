package io.polychro.markdown;

import io.polychro.spi.Validator;
import io.polychro.spi.ValidatorConfig;
import io.polychro.spi.ValidatorFactory;

/**
 * Factory for creating {@link MarkdownValidator} instances.
 */
public class MarkdownValidatorFactory implements ValidatorFactory {

    @Override
    public String name() {
        return MarkdownValidator.NAME;
    }

    @Override
    public Validator create(ValidatorConfig config) {
        int lineLength = config.get("lineLength", Integer.class)
                .orElse(MarkdownValidator.DEFAULT_LINE_LENGTH);
        String listMarker = config.get("listMarker", String.class)
                .orElse(MarkdownValidator.DEFAULT_LIST_MARKER);
        String formatOverride = config.get("format", String.class).orElse(null);

        MarkdownFormat format;
        if (formatOverride != null) {
            format = FormatDetector.fromName(formatOverride);
        } else {
            // Format will be detected per-document; use generic as default
            format = new GenericFormat();
        }

        return new MarkdownValidator(lineLength, listMarker, format, new FrontmatterParser());
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

        MarkdownFormat format;
        if (formatOverride != null) {
            format = FormatDetector.fromName(formatOverride);
        } else {
            format = FormatDetector.detect(sourcePath);
        }

        return new MarkdownValidator(lineLength, listMarker, format, new FrontmatterParser());
    }
}
