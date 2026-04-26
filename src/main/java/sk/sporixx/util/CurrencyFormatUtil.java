package sk.sporixx.util;

import java.text.NumberFormat;
import java.util.Locale;
import sk.sporixx.service.ServiceLocator;

public final class CurrencyFormatUtil {

    private CurrencyFormatUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String format(double value) {
        var settingsService = ServiceLocator.getSettingsService();
        String languageCode = settingsService.getLanguageCode();
        String currencyCode = settingsService.getCurrencyCode();

        Locale locale = switch (languageCode) {
            case "sk" -> Locale.forLanguageTag("sk-SK");
            case "cs" -> Locale.forLanguageTag("cs-CZ");
            default -> Locale.US;
        };

        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(java.util.Currency.getInstance(currencyCode));
        return formatter.format(value);
    }
}
