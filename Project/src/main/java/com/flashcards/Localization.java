package com.flashcards;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;

public final class Localization {
    private Locale locale = Locale.ENGLISH;
    private ResourceBundle messages = ResourceBundle.getBundle("com.flashcards.messages", locale);

    public void setLanguage(String languageTag) {
        Locale selected = Locale.forLanguageTag(languageTag);
        if (!java.util.Set.of("en", "ja", "ru", "ar").contains(selected.getLanguage())) {
            throw new IllegalArgumentException("Unsupported language: " + languageTag);
        }
        locale = selected;
        messages = ResourceBundle.getBundle("com.flashcards.messages", locale);
    }

    public Locale locale() {
        return locale;
    }

    public String text(String key, Object... arguments) {
        String pattern = messages.getString(key);
        return arguments.length == 0 ? pattern : new MessageFormat(pattern, locale).format(arguments);
    }

    public String number(Number number) {
        return NumberFormat.getIntegerInstance(locale).format(number);
    }

    public String decimal(Number number) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format(number);
    }

    public String dateTime(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    public boolean isRightToLeft() {
        return "ar".equals(locale.getLanguage());
    }
}
