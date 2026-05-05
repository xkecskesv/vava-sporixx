package sk.sporixx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.UserSettings;
import sk.sporixx.repository.SettingsRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testy pre SettingsService.
 *
 * Stratégia: SettingsRepository sa stubuje InMemory implementáciou –
 * žiadna DB, žiadny súborový systém.
 * reload() sa netestuje (volá Localization.setLanguage → závisí od bundle).
 */
@DisplayName("SettingsService")
class SettingsServiceTest {

    private InMemorySettingsRepository repo;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        repo = new InMemorySettingsRepository();
        service = new SettingsServiceImpl(repo);
    }

    // ─── Defaults ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Defaultné hodnoty")
    class Defaults {

        @Test
        @DisplayName("prázdne repo → language je 'en'")
        void defaults_language_en() {
            assertEquals("en", service.getLanguageCode());
        }

        @Test
        @DisplayName("prázdne repo → currency je 'EUR'")
        void defaults_currency_eur() {
            assertEquals("EUR", service.getCurrencyCode());
        }

    }

    // ─── setLanguageCode ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("setLanguageCode")
    class SetLanguage {

        @Test
        @DisplayName("platný kód 'sk' sa uloží")
        void setLanguage_sk_persisted() {
            service.setLanguageCode("sk");
            assertEquals("sk", service.getLanguageCode());
        }

        @Test
        @DisplayName("platný kód 'de' sa uloží")
        void setLanguage_de_persisted() {
            service.setLanguageCode("de");
            assertEquals("de", service.getLanguageCode());
        }

        @Test
        @DisplayName("platný kód 'cs' sa uloží")
        void setLanguage_cs_persisted() {
            service.setLanguageCode("cs");
            assertEquals("cs", service.getLanguageCode());
        }

        @Test
        @DisplayName("platný kód 'pl' sa uloží")
        void setLanguage_pl_persisted() {
            service.setLanguageCode("pl");
            assertEquals("pl", service.getLanguageCode());
        }

        @Test
        @DisplayName("neznámy kód → fallback na 'en'")
        void setLanguage_unknown_fallbackToEn() {
            service.setLanguageCode("xx");
            assertEquals("en", service.getLanguageCode());
        }

        @Test
        @DisplayName("null → fallback na 'en'")
        void setLanguage_null_fallbackToEn() {
            service.setLanguageCode(null);
            assertEquals("en", service.getLanguageCode());
        }

        @Test
        @DisplayName("veľké písmená sa normalizujú (SK → sk)")
        void setLanguage_uppercase_normalized() {
            service.setLanguageCode("SK");
            assertEquals("sk", service.getLanguageCode());
        }

        @Test
        @DisplayName("zmena sa persistuje do repo")
        void setLanguage_persistsToRepo() {
            service.setLanguageCode("sk");
            assertEquals("sk", repo.lastSaved.getLanguageCode());
        }
    }

    // ─── setCurrencyCode ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("setCurrencyCode")
    class SetCurrency {

        @Test
        @DisplayName("platný kód 'USD' sa uloží")
        void setCurrency_usd_persisted() {
            service.setCurrencyCode("USD");
            assertEquals("USD", service.getCurrencyCode());
        }

        @Test
        @DisplayName("platný kód 'CZK' sa uloží")
        void setCurrency_czk_persisted() {
            service.setCurrencyCode("CZK");
            assertEquals("CZK", service.getCurrencyCode());
        }

        @Test
        @DisplayName("platný kód 'GBP' sa uloží")
        void setCurrency_gbp_persisted() {
            service.setCurrencyCode("GBP");
            assertEquals("GBP", service.getCurrencyCode());
        }

        @Test
        @DisplayName("platný kód 'PLN' sa uloží")
        void setCurrency_pln_persisted() {
            service.setCurrencyCode("PLN");
            assertEquals("PLN", service.getCurrencyCode());
        }

        @Test
        @DisplayName("neznámy kód → fallback na 'EUR'")
        void setCurrency_unknown_fallbackToEur() {
            service.setCurrencyCode("XYZ");
            assertEquals("EUR", service.getCurrencyCode());
        }

        @Test
        @DisplayName("null → fallback na 'EUR'")
        void setCurrency_null_fallbackToEur() {
            service.setCurrencyCode(null);
            assertEquals("EUR", service.getCurrencyCode());
        }

        @Test
        @DisplayName("malé písmená sa normalizujú (eur → EUR)")
        void setCurrency_lowercase_normalized() {
            service.setCurrencyCode("eur");
            assertEquals("EUR", service.getCurrencyCode());
        }

        @Test
        @DisplayName("zmena sa persistuje do repo")
        void setCurrency_persistsToRepo() {
            service.setCurrencyCode("USD");
            assertEquals("USD", repo.lastSaved.getCurrencyCode());
        }
    }

    // ─── getSettingsSnapshot ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getSettingsSnapshot")
    class Snapshot {

        @Test
        @DisplayName("snapshot obsahuje aktuálny language kód")
        void snapshot_containsCurrentLanguage() {
            service.setLanguageCode("sk");
            assertEquals("sk", service.getSettingsSnapshot().getLanguageCode());
        }

        @Test
        @DisplayName("snapshot obsahuje aktuálny currency kód")
        void snapshot_containsCurrentCurrency() {
            service.setCurrencyCode("USD");
            assertEquals("USD", service.getSettingsSnapshot().getCurrencyCode());
        }

        @Test
        @DisplayName("snapshot je kópia – zmena snapshottu neovplyvní service")
        void snapshot_isCopy_notLive() {
            service.setLanguageCode("sk");
            UserSettings snap = service.getSettingsSnapshot();
            // zmeníme snapshot (ak je immutable, nič sa nestane; ak nie, je to kópia)
            service.setLanguageCode("de");
            // original snapshot stále ukazuje sk
            assertEquals("sk", snap.getLanguageCode());
        }

    }

    // ─── Načítanie z repo pri štarte ──────────────────────────────────────────

    @Nested
    @DisplayName("Načítanie z repo pri štarte")
    class LoadFromRepo {

        @Test
        @DisplayName("ak repo vráti settings, service ich použije")
        void load_fromRepo_usedOnInit() {
            UserSettings stored = UserSettings.builder()
                    .languageCode("pl")
                    .currencyCode("PLN")
                    .build();
            InMemorySettingsRepository preloaded = new InMemorySettingsRepository(stored);
            SettingsService svc = new SettingsServiceImpl(preloaded);

            assertEquals("pl", svc.getLanguageCode());
            assertEquals("PLN", svc.getCurrencyCode());
        }

        @Test
        @DisplayName("ak repo hodí výnimku, použijú sa defaults")
        void load_repoThrows_fallbackToDefaults() {
            SettingsRepository failing = new SettingsRepository() {
                @Override public UserSettings load() { throw new RuntimeException("DB error"); }
                @Override public void save(UserSettings s) {}
            };
            SettingsService svc = new SettingsServiceImpl(failing);
            assertEquals("en", svc.getLanguageCode());
            assertEquals("EUR", svc.getCurrencyCode());
        }

        @Test
        @DisplayName("neznámy language z repo sa sanitizuje na 'en'")
        void load_unknownLanguageInRepo_sanitized() {
            UserSettings stored = UserSettings.builder()
                    .languageCode("zz").currencyCode("EUR").build();
            SettingsService svc = new SettingsServiceImpl(new InMemorySettingsRepository(stored));
            assertEquals("en", svc.getLanguageCode());
        }

        @Test
        @DisplayName("neznáma currency z repo sa sanitizuje na 'EUR'")
        void load_unknownCurrencyInRepo_sanitized() {
            UserSettings stored = UserSettings.builder()
                    .languageCode("en").currencyCode("ZZZ").build();
            SettingsService svc = new SettingsServiceImpl(new InMemorySettingsRepository(stored));
            assertEquals("EUR", svc.getCurrencyCode());
        }
    }

    // ─── InMemory SettingsRepository stub ────────────────────────────────────

    private static class InMemorySettingsRepository implements SettingsRepository {

        private UserSettings stored;
        UserSettings lastSaved = null;

        InMemorySettingsRepository() {
            this.stored = null; // prázdne repo → service použije defaults
        }

        InMemorySettingsRepository(UserSettings stored) {
            this.stored = stored;
        }

        @Override
        public UserSettings load() {
            if (stored == null) throw new RuntimeException("No settings in repo");
            return stored;
        }

        @Override
        public void save(UserSettings settings) {
            this.stored = settings;
            this.lastSaved = settings;
        }
    }
}

