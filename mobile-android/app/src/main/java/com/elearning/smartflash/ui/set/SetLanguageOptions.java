package com.elearning.smartflash.ui.set;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Shared language definitions for Terms and Definitions in Create/Edit Set & Set Options.
 */
public final class SetLanguageOptions {

    public static final class LanguageEntry {
        public final String code;
        public final String label;

        public LanguageEntry(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    public static final List<LanguageEntry> TOP_LANGUAGES = Arrays.asList(
            new LanguageEntry("en", "English"),
            new LanguageEntry("vi", "Vietnamese"),
            new LanguageEntry("es", "Spanish"),
            new LanguageEntry("fr", "French"),
            new LanguageEntry("it", "Italian"),
            new LanguageEntry("de", "German"),
            new LanguageEntry("ru", "Russian"),
            new LanguageEntry("ja", "Japanese"),
            new LanguageEntry("ja-ro", "Japanese (Romaji)"),
            new LanguageEntry("zh", "Chinese (Simplified)"),
            new LanguageEntry("zh-TW", "Chinese (Traditional)"),
            new LanguageEntry("zh-pi", "Chinese (Pinyin)"),
            new LanguageEntry("ko", "Korean"),
            new LanguageEntry("math", "Math / Symbols"),
            new LanguageEntry("chem", "Chemistry")
    );

    public static final List<LanguageEntry> OTHER_LANGUAGES = Arrays.asList(
            new LanguageEntry("pt", "Portuguese"),
            new LanguageEntry("ar", "Arabic"),
            new LanguageEntry("th", "Thai"),
            new LanguageEntry("id", "Indonesian"),
            new LanguageEntry("", "None (Clear selection)")
    );

    private SetLanguageOptions() {
    }

    public static String getDisplayName(String code, String fallback) {
        if (code == null || code.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = code.trim();
        for (LanguageEntry entry : TOP_LANGUAGES) {
            if (entry.code.equalsIgnoreCase(trimmed)) {
                return entry.label;
            }
        }
        for (LanguageEntry entry : OTHER_LANGUAGES) {
            if (!entry.code.isEmpty() && entry.code.equalsIgnoreCase(trimmed)) {
                return entry.label;
            }
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public static String getInlineButtonLabel(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "SELECT LANGUAGE";
        }
        return getDisplayName(code, "SELECT LANGUAGE").toUpperCase(Locale.ROOT);
    }
}
