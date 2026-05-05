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

    /**
     * Konvertuje sumu zo zadávacej meny používateľa do EUR pre uloženie do DB.
     * Používa delenie kurzu EUR→userCurrency, čím je zaručená invertovateľnosť
     * s fromEur() bez ohľadu na prípadnú nekonzistentnosť kurzov v DB.
     */
    public static double toEur(double amount) {
        CurrencyService cs = ServiceLocator.getCurrencyService();
        String userCurrency = cs.getUserCurrency();
        if ("EUR".equals(userCurrency)) return amount;
        double eurToUser = cs.convert(1.0, "EUR", userCurrency);
        return eurToUser == 0 ? amount : amount / eurToUser;
    }

    /**
     * Konvertuje sumu z EUR (uloženú v DB) do meny používateľa pre zobrazenie v inpute.
     */
    public static double fromEur(double amount) {
        CurrencyService cs = ServiceLocator.getCurrencyService();
        String userCurrency = cs.getUserCurrency();
        if ("EUR".equals(userCurrency)) return amount;
        return cs.convert(amount, "EUR", userCurrency);
    }
}