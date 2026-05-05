package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.UserSettings;
import sk.sporixx.repository.SettingsRepositoryImpl;
import sk.sporixx.repository.SettingsRepository;

import sk.sporixx.util.Localization;

import java.util.Locale;
import java.util.Set;

public class SettingsServiceImpl implements SettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_CURRENCY = "EUR";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "sk", "cs", "de", "pl");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "USD", "CZK", "GBP", "PLN");

    private final SettingsRepository repository;
    private UserSettings currentSettings;

    public SettingsServiceImpl() {
        this(new SettingsRepositoryImpl());
    }

    SettingsServiceImpl(SettingsRepository repository) {
        this.repository = repository;
        this.currentSettings = loadFromRepository();
    }

    private UserSettings loadFromRepository() {
        try {
            UserSettings loaded = repository.load();
            return sanitize(loaded);
        } catch (Exception e) {
            logger.error("Failed to load settings from repository, using defaults", e);
            return defaultSettings();
        }
    }

    @Override
    public String getLanguageCode() {
        return currentSettings.getLanguageCode();
    }

    @Override
    public void setLanguageCode(String languageCode) {
        currentSettings = UserSettings.builder()
                .languageCode(normalizeLanguage(languageCode))
                .currencyCode(currentSettings.getCurrencyCode())
                .build();
        persist();
    }

    @Override
    public String getCurrencyCode() {
        return currentSettings.getCurrencyCode();
    }

    @Override
    public void setCurrencyCode(String currencyCode) {
        currentSettings = UserSettings.builder()
                .languageCode(currentSettings.getLanguageCode())
                .currencyCode(normalizeCurrency(currencyCode))
                .build();
        persist();
    }

    @Override
    public UserSettings getSettingsSnapshot() {
        return UserSettings.builder()
                .languageCode(currentSettings.getLanguageCode())
                .currencyCode(currentSettings.getCurrencyCode())
                .build();
    }

    @Override
    public void reload() {
        this.currentSettings = loadFromRepository();
        Localization.setLanguage(currentSettings.getLanguageCode());
    }

    private void persist() {
        try {
            repository.save(currentSettings);
        } catch (Exception e) {
            logger.error("Failed to persist settings", e);
        }
    }

    private UserSettings sanitize(UserSettings settings) {
        if (settings == null) return defaultSettings();
        return UserSettings.builder()
                .languageCode(normalizeLanguage(settings.getLanguageCode()))
                .currencyCode(normalizeCurrency(settings.getCurrencyCode()))
                .build();
    }

    private String normalizeLanguage(String code) {
        String normalized = code == null ? DEFAULT_LANGUAGE : code.toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE;
    }

    private String normalizeCurrency(String code) {
        String normalized = code == null ? DEFAULT_CURRENCY : code.toUpperCase(Locale.ROOT);
        return SUPPORTED_CURRENCIES.contains(normalized) ? normalized : DEFAULT_CURRENCY;
    }

    private UserSettings defaultSettings() {
        return UserSettings.builder()
                .languageCode(DEFAULT_LANGUAGE)
                .currencyCode(DEFAULT_CURRENCY)
                .build();
    }
}