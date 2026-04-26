package sk.sporixx.service;

import java.util.Locale;
import java.util.Set;
import java.util.prefs.Preferences;
import sk.sporixx.dto.UserSettings;

public class SettingsServiceImpl implements SettingsService {

    private static final String PREF_NODE = "sk/sporixx/settings";

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_CURRENCY = "currency";

    private static final String KEY_UPCOMING = "notifications.upcoming";
    private static final String KEY_BUDGET = "notifications.budget";
    private static final String KEY_REMINDERS = "notifications.reminders";
    private static final String KEY_GOALS = "notifications.goals";
    private static final String KEY_ACHIEVEMENTS = "notifications.achievements";

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_CURRENCY = "EUR";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "sk", "cs");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "USD", "CZK");

    private final Preferences preferences = Preferences.userRoot().node(PREF_NODE);

    @Override
    public String getLanguageCode() {
        String language = preferences.get(KEY_LANGUAGE, DEFAULT_LANGUAGE).toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(language) ? language : DEFAULT_LANGUAGE;
    }

    @Override
    public void setLanguageCode(String languageCode) {
        String normalized = languageCode == null
                ? DEFAULT_LANGUAGE
                : languageCode.toLowerCase(Locale.ROOT);
        preferences.put(KEY_LANGUAGE,
                SUPPORTED_LANGUAGES.contains(normalized) ? normalized : DEFAULT_LANGUAGE);
    }

    @Override
    public String getCurrencyCode() {
        String currency = preferences.get(KEY_CURRENCY, DEFAULT_CURRENCY).toUpperCase(Locale.ROOT);
        return SUPPORTED_CURRENCIES.contains(currency) ? currency : DEFAULT_CURRENCY;
    }

    @Override
    public void setCurrencyCode(String currencyCode) {
        String normalized = currencyCode == null
                ? DEFAULT_CURRENCY
                : currencyCode.toUpperCase(Locale.ROOT);
        preferences.put(KEY_CURRENCY,
                SUPPORTED_CURRENCIES.contains(normalized) ? normalized : DEFAULT_CURRENCY);
    }

    @Override
    public boolean isUpcomingPaymentsEnabled() {
        return preferences.getBoolean(KEY_UPCOMING, true);
    }

    @Override
    public void setUpcomingPaymentsEnabled(boolean enabled) {
        preferences.putBoolean(KEY_UPCOMING, enabled);
    }

    @Override
    public boolean isBudgetLimitAlertsEnabled() {
        return preferences.getBoolean(KEY_BUDGET, true);
    }

    @Override
    public void setBudgetLimitAlertsEnabled(boolean enabled) {
        preferences.putBoolean(KEY_BUDGET, enabled);
    }

    @Override
    public boolean isSavingRemindersEnabled() {
        return preferences.getBoolean(KEY_REMINDERS, true);
    }

    @Override
    public void setSavingRemindersEnabled(boolean enabled) {
        preferences.putBoolean(KEY_REMINDERS, enabled);
    }

    @Override
    public boolean isSavingGoalsUpdatesEnabled() {
        return preferences.getBoolean(KEY_GOALS, true);
    }

    @Override
    public void setSavingGoalsUpdatesEnabled(boolean enabled) {
        preferences.putBoolean(KEY_GOALS, enabled);
    }

    @Override
    public boolean isAchievementsEnabled() {
        return preferences.getBoolean(KEY_ACHIEVEMENTS, true);
    }

    @Override
    public void setAchievementsEnabled(boolean enabled) {
        preferences.putBoolean(KEY_ACHIEVEMENTS, enabled);
    }

    @Override
    public UserSettings getSettingsSnapshot() {
        return UserSettings.builder()
                .languageCode(getLanguageCode())
                .currencyCode(getCurrencyCode())
                .upcomingPaymentsEnabled(isUpcomingPaymentsEnabled())
                .budgetLimitAlertsEnabled(isBudgetLimitAlertsEnabled())
                .savingRemindersEnabled(isSavingRemindersEnabled())
                .savingGoalsUpdatesEnabled(isSavingGoalsUpdatesEnabled())
                .achievementsEnabled(isAchievementsEnabled())
                .build();
    }
}

