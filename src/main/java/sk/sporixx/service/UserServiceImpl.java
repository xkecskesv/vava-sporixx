package sk.sporixx.service;

import sk.sporixx.dto.CurrentUser;
import sk.sporixx.model.GenderCode;
import sk.sporixx.util.Localization;

/**
 * Default implementation of {@link UserService}.
 *
 * <p>Provides current-user access and centralized gender normalization/display mapping.</p>
 *
 * @author Viktória Kecskés
 */
public class UserServiceImpl implements UserService {

    /**
     * Returns current session user as a DTO visible to the UI layer.
     *
     * @return current user DTO, or {@code null} when not authenticated
     */
    @Override
    public CurrentUser getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }

    /**
     * Normalizes arbitrary gender input into canonical persistence values.
     *
     * @param rawGender raw value from persistence or UI
     * @return {@code M}, {@code F}, or {@code ONHSR}
     */
    @Override
    public String normalizeGender(String rawGender) {
        if (rawGender == null || rawGender.isBlank()) {
            return GenderCode.UNKNOWN;
        }

        String normalized = rawGender.trim().toLowerCase();
        if (normalized.startsWith("m")) {
            return GenderCode.MALE;
        }
        if (normalized.startsWith("f") || normalized.startsWith("z")) {
            return GenderCode.FEMALE;
        }
        return GenderCode.UNKNOWN;
    }

    /**
     * Maps raw gender to localized ComboBox text with fallback.
     *
     * @param rawGender raw value from persistence
     * @return localized label or {@code "-"} when unknown
     */
    @Override
    public String toDisplayGender(String rawGender) {
        String normalized = normalizeGender(rawGender);
        if (GenderCode.MALE.equals(normalized)) {
            return Localization.get("profile.information.gender_male");
        }
        if (GenderCode.FEMALE.equals(normalized)) {
            return Localization.get("profile.information.gender_female");
        }
        return "-";
    }
}

