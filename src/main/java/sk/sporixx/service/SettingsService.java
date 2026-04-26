package sk.sporixx.service;

import sk.sporixx.dto.UserSettings;

public interface SettingsService {

    String getLanguageCode();

    void setLanguageCode(String languageCode);

    String getCurrencyCode();

    void setCurrencyCode(String currencyCode);

    boolean isUpcomingPaymentsEnabled();

    void setUpcomingPaymentsEnabled(boolean enabled);

    boolean isBudgetLimitAlertsEnabled();

    void setBudgetLimitAlertsEnabled(boolean enabled);

    boolean isSavingRemindersEnabled();

    void setSavingRemindersEnabled(boolean enabled);

    boolean isSavingGoalsUpdatesEnabled();

    void setSavingGoalsUpdatesEnabled(boolean enabled);

    boolean isAchievementsEnabled();

    void setAchievementsEnabled(boolean enabled);

    UserSettings getSettingsSnapshot();

    void reload();
}

