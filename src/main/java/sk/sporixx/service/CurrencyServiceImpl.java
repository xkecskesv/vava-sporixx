package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.util.DatabaseConfig;

import java.sql.*;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyServiceImpl implements CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);

    private final SettingsService settingsService;
    private final Map<String, Double> ratesCache = new ConcurrentHashMap<>();

    public CurrencyServiceImpl(SettingsService settingsService) {
        this.settingsService = settingsService;
        refreshRates();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
    }

    @Override
    public void refreshRates() {
        String sql = """
            SELECT base_currency_code, target_currency_code, rate
            FROM exchange_rates
            ORDER BY captured_at DESC
        """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            Map<String, Double> loaded = new ConcurrentHashMap<>();
            while (rs.next()) {
                String base = rs.getString("base_currency_code");
                String target = rs.getString("target_currency_code");
                double rate = rs.getDouble("rate");
                // Kľúč: "EUR_USD" = koľko USD je 1 EUR
                loaded.put(base + "_" + target, rate);
            }

            if (!loaded.isEmpty()) {
                ratesCache.clear();
                ratesCache.putAll(loaded);
                logger.info("Exchange rates loaded from DB: {} pairs", loaded.size());
            } else {
                logger.warn("No exchange rates found in DB, cache unchanged");
            }

        } catch (SQLException e) {
            logger.error("Failed to load exchange rates from DB", e);
        }
    }

    @Override
    public double convert(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) return amount;

        if (ratesCache.isEmpty()) {
            logger.warn("No rates available, returning original amount");
            return amount;
        }

        // Priamy kurz: napr. EUR_USD
        String directKey = fromCurrency + "_" + toCurrency;
        if (ratesCache.containsKey(directKey)) {
            return amount * ratesCache.get(directKey);
        }

        // Nepriamy cez EUR: from→EUR→to
        String toEurKey = fromCurrency + "_EUR";
        String fromEurKey = "EUR_" + toCurrency;

        if (ratesCache.containsKey(toEurKey) && ratesCache.containsKey(fromEurKey)) {
            double inEur = amount * ratesCache.get(toEurKey);
            return inEur * ratesCache.get(fromEurKey);
        }

        logger.warn("No conversion rate found for {}→{}, returning original", fromCurrency, toCurrency);
        return amount;
    }

    @Override
    public String format(double amount, String targetCurrency) {
        if (ratesCache.isEmpty()) {
            return String.format("%.2f %s (!)", amount, targetCurrency);
        }
        try {
            // Mena určuje locale (formát čísla + symbol).
            // EUR je výnimka — používa sa vo viacerých krajinách, preto sleduje jazyk aplikácie.
            Locale locale = localeForCurrency(targetCurrency);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
            formatter.setCurrency(Currency.getInstance(targetCurrency));

            // Symbol sa nastaví explicitne, aby bol správny bez ohľadu na locale.
            if (formatter instanceof java.text.DecimalFormat df) {
                java.text.DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
                dfs.setCurrencySymbol(symbolForCurrency(targetCurrency));
                df.setDecimalFormatSymbols(dfs);
            }

            return formatter.format(amount);

        } catch (Exception e) {
            logger.warn("Failed to format currency {}: {}", targetCurrency, e.getMessage());
            return String.format("%.2f %s", amount, targetCurrency);
        }
    }

    private Locale localeForCurrency(String currencyCode) {
        return switch (currencyCode) {
            case "CZK" -> Locale.forLanguageTag("cs-CZ");
            case "PLN" -> Locale.forLanguageTag("pl-PL");
            case "USD" -> Locale.US;
            case "GBP" -> Locale.UK;
            default    -> localeForLanguage(settingsService.getLanguageCode());
        };
    }

    private String symbolForCurrency(String currencyCode) {
        return switch (currencyCode) {
            case "EUR" -> "€";
            case "USD" -> "$";
            case "GBP" -> "£";
            case "CZK" -> "Kč";
            case "PLN" -> "zł";
            default    -> currencyCode;
        };
    }

    private Locale localeForLanguage(String langCode) {
        return switch (langCode) {
            case "sk" -> Locale.forLanguageTag("sk-SK");
            case "cs" -> Locale.forLanguageTag("cs-CZ");
            case "de" -> Locale.forLanguageTag("de-DE");
            case "pl" -> Locale.forLanguageTag("pl-PL");
            default   -> Locale.US;
        };
    }

    @Override
    public String getUserCurrency() {
        return settingsService.getCurrencyCode();
    }
}