package sk.sporixx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testy pre CurrencyService – convert(), format(), getUserCurrency().
 *
 * Keďže refreshRates() načítava z DB, testy používajú reflexiu na priame
 * naplnenie ratesCache testovacími kurzami.
 */
@DisplayName("CurrencyService")
class CurrencyServiceTest {

    /** Minimálny stub SettingsService pre testy. */
    private static final SettingsService SETTINGS_EN = new StubSettingsService("en", "EUR");
    private static final SettingsService SETTINGS_SK = new StubSettingsService("sk", "EUR");

    // ─── helper: vytvor CurrencyServiceImpl bez DB, s injektovanými kurzami ───

    /**
     * Vytvorí CurrencyServiceImpl, pričom refreshRates() sa NEODVOLA (k DB).
     * Kurzy sa injektujú priamo do ratesCache cez reflexiu.
     */
    private CurrencyServiceImpl buildService(SettingsService settings, Map<String, Double> rates) {
        // Subclass – overridneme refreshRates() aby nekontaktovala DB
        CurrencyServiceImpl svc = new CurrencyServiceImpl(settings) {
            @Override
            public void refreshRates() { /* no-op – nepristupujeme k DB */ }
        };
        try {
            Field cacheField = CurrencyServiceImpl.class.getDeclaredField("ratesCache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Double> cache = (Map<String, Double>) cacheField.get(svc);
            cache.putAll(rates);
        } catch (Exception e) {
            throw new RuntimeException("Reflexia zlyhala", e);
        }
        return svc;
    }

    private CurrencyServiceImpl buildServiceEmpty(SettingsService settings) {
        return buildService(settings, Map.of());
    }

    // ─── convert() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convert()")
    class Convert {

        private CurrencyServiceImpl svc;

        @BeforeEach
        void setUp() {
            svc = buildService(SETTINGS_EN, Map.of(
                    "EUR_USD", 1.08,
                    "EUR_CZK", 25.0,
                    "USD_EUR", 0.925,
                    "CZK_EUR", 0.04
            ));
        }

        @Test
        @DisplayName("rovnaká mena → rovnaká suma (bez konverzie)")
        void convert_sameCurrency_returnsOriginal() {
            assertEquals(100.0, svc.convert(100.0, "EUR", "EUR"));
        }

        @Test
        @DisplayName("priamy kurz EUR→USD")
        void convert_direct_eurToUsd() {
            assertEquals(108.0, svc.convert(100.0, "EUR", "USD"), 0.001);
        }

        @Test
        @DisplayName("priamy kurz EUR→CZK")
        void convert_direct_eurToCzk() {
            assertEquals(2500.0, svc.convert(100.0, "EUR", "CZK"), 0.001);
        }

        @Test
        @DisplayName("nepriamy kurz cez EUR: USD→CZK")
        void convert_indirect_usdToCzk() {
            // 100 USD → 92.5 EUR → 2312.5 CZK
            double result = svc.convert(100.0, "USD", "CZK");
            assertEquals(100.0 * 0.925 * 25.0, result, 0.01);
        }

        @Test
        @DisplayName("nulová suma konvertuje na nulu")
        void convert_zeroAmount_returnsZero() {
            assertEquals(0.0, svc.convert(0.0, "EUR", "USD"));
        }

        @Test
        @DisplayName("záporná suma sa konvertuje správne")
        void convert_negativeAmount_converted() {
            assertEquals(-108.0, svc.convert(-100.0, "EUR", "USD"), 0.001);
        }

        @Test
        @DisplayName("prázdna cache → vráti pôvodnú sumu + log warning")
        void convert_emptyCache_returnsOriginal() {
            CurrencyServiceImpl emptySvc = buildServiceEmpty(SETTINGS_EN);
            assertEquals(50.0, emptySvc.convert(50.0, "EUR", "USD"));
        }

        @Test
        @DisplayName("chýbajúci kurz → vráti pôvodnú sumu")
        void convert_missingRate_returnsOriginal() {
            assertEquals(100.0, svc.convert(100.0, "EUR", "JPY"));
        }
    }

    // ─── format() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("format()")
    class Format {

        @Test
        @DisplayName("EUR formátovanie obsahuje € symbol")
        void format_eur_containsEuroSign() {
            CurrencyServiceImpl svc = buildService(SETTINGS_EN, Map.of("EUR_USD", 1.08));
            String result = svc.format(100.0, "EUR");
            assertTrue(result.contains("€"), "Výsledok má obsahovať €, bol: " + result);
        }

        @Test
        @DisplayName("USD formátovanie obsahuje $ symbol")
        void format_usd_containsDollarSign() {
            CurrencyServiceImpl svc = buildService(SETTINGS_EN, Map.of("EUR_USD", 1.08));
            String result = svc.format(100.0, "USD");
            assertTrue(result.contains("$"), "Výsledok má obsahovať $, bol: " + result);
        }

        @Test
        @DisplayName("CZK formátovanie obsahuje Kč symbol")
        void format_czk_containsKc() {
            CurrencyServiceImpl svc = buildService(SETTINGS_EN, Map.of("EUR_CZK", 25.0));
            String result = svc.format(100.0, "CZK");
            assertTrue(result.contains("Kč"), "Výsledok má obsahovať Kč, bol: " + result);
        }

        @Test
        @DisplayName("PLN formátovanie obsahuje zł symbol")
        void format_pln_containsZloty() {
            CurrencyServiceImpl svc = buildService(SETTINGS_EN, Map.of("EUR_PLN", 4.3));
            String result = svc.format(100.0, "PLN");
            assertTrue(result.contains("zł"), "Výsledok má obsahovať zł, bol: " + result);
        }

        @Test
        @DisplayName("GBP formátovanie obsahuje £ symbol")
        void format_gbp_containsPound() {
            CurrencyServiceImpl svc = buildService(SETTINGS_EN, Map.of("EUR_GBP", 0.85));
            String result = svc.format(100.0, "GBP");
            assertTrue(result.contains("£"), "Výsledok má obsahovať £, bol: " + result);
        }

        @Test
        @DisplayName("prázdna cache → fallback formát 'X.XX MENA (!)'")
        void format_emptyCache_fallbackFormat() {
            CurrencyServiceImpl svc = buildServiceEmpty(SETTINGS_EN);
            String result = svc.format(42.5, "EUR");
            assertTrue(result.contains("!"), "Fallback má obsahovať (!), bol: " + result);
            assertTrue(result.contains("42"), "Fallback má obsahovať číslo, bol: " + result);
        }

        @Test
        @DisplayName("sk locale – EUR formátovanie stále obsahuje €")
        void format_skLocale_eur_containsEuroSign() {
            CurrencyServiceImpl svc = buildService(SETTINGS_SK, Map.of("EUR_USD", 1.08));
            String result = svc.format(100.0, "EUR");
            assertTrue(result.contains("€"), "Výsledok má obsahovať €, bol: " + result);
        }
    }

    // ─── getUserCurrency() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserCurrency()")
    class GetUserCurrency {

        @Test
        @DisplayName("vráti kód meny zo SettingsService")
        void getUserCurrency_returnsSettingsCurrency() {
            CurrencyServiceImpl svc = buildService(new StubSettingsService("en", "USD"), Map.of());
            assertEquals("USD", svc.getUserCurrency());
        }

        @Test
        @DisplayName("EUR mena vrátená správne")
        void getUserCurrency_eur() {
            CurrencyServiceImpl svc = buildService(SETTINGS_EN, Map.of());
            assertEquals("EUR", svc.getUserCurrency());
        }
    }

    // ─── Stub SettingsService ─────────────────────────────────────────────────

    private static class StubSettingsService implements SettingsService {
        private final String lang;
        private final String currency;

        StubSettingsService(String lang, String currency) {
            this.lang = lang;
            this.currency = currency;
        }

        @Override public String getLanguageCode()  { return lang; }
        @Override public String getCurrencyCode()  { return currency; }

        // ostatné metódy – no-op / default
        @Override public void setLanguageCode(String c) {}
        @Override public void setCurrencyCode(String c) {}
        @Override public sk.sporixx.dto.UserSettings getSettingsSnapshot() { return null; }
        @Override public void reload() {}
    }
}


