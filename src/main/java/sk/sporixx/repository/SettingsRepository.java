package sk.sporixx.repository;

import sk.sporixx.dto.UserSettings;

public interface SettingsRepository {
    UserSettings load();
    void save(UserSettings settings);
}