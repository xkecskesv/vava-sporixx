package sk.sporixx.service;

import sk.sporixx.dto.CurrentUser;

/**
 * User-related read utilities and normalization helpers used by UI/services.
 */
public interface UserService {

    /** Returns current session user as UI-safe DTO, or null when not logged in. */
    CurrentUser getCurrentUser();

    /** Normalizes raw gender value to canonical storage values: M, F, ONHSR. */
    String normalizeGender(String rawGender);

    /** Maps raw/canonical gender value to localized ComboBox value, or '-' fallback. */
    String toDisplayGender(String rawGender);
}

