package sk.sporixx.service;

import sk.sporixx.dto.UserSettings;

public interface SettingsService {

    String getLanguageCode();

    void setLanguageCode(String languageCode);

    String getCurrencyCode();

    void setCurrencyCode(String currencyCode);

    UserSettings getSettingsSnapshot();

    void reload();
}

