package sk.sporixx.util;

import sk.sporixx.service.CurrencyService;
import sk.sporixx.service.ServiceLocator;

public final class CurrencyFormatUtil {

    private CurrencyFormatUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Konvertuje sumu z EUR do meny nastavenej používateľom a naformátuje ju.
     * Volá sa všade tam, kde sa zobrazuje peňažná hodnota.
     *
     * @param amount suma v EUR
     * @return naformátovaný reťazec v používateľovej mene
     */
    public static String format(double amount) {
        CurrencyService currencyService = ServiceLocator.getCurrencyService();
        String targetCurrency = currencyService.getUserCurrency();
        double converted = currencyService.convert(amount, "EUR", targetCurrency);
        return currencyService.format(converted, targetCurrency);
    }

    /**
     * Konvertuje sumu zo zadanej zdrojovej meny do meny nastavenej používateľom.
     * Používa sa ak zdrojová suma nie je v EUR.
     *
     * @param amount suma v zdrojovej mene
     * @param fromCurrency kód zdrojovej meny (napr. "USD", "CZK")
     * @return naformátovaný reťazec v používateľovej mene
     */
    public static String formatFrom(double amount, String fromCurrency) {
        CurrencyService currencyService = ServiceLocator.getCurrencyService();
        String targetCurrency = currencyService.getUserCurrency();
        double converted = currencyService.convert(amount, fromCurrency, targetCurrency);
        return currencyService.format(converted, targetCurrency);
    }
}