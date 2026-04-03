package sk.sporixx.service;

import sk.sporixx.dto.CurrentUser;
import sk.sporixx.util.Localization;

/**
 * Default implementation of UserService.
 */
public class UserServiceImpl implements UserService {

    private static final String GENDER_UNKNOWN = "ONHSR";

    @Override
    public CurrentUser getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }

    @Override
    public String normalizeGender(String rawGender) {
        if (rawGender == null || rawGender.isBlank()) {
            return GENDER_UNKNOWN;
        }

        String normalized = rawGender.trim().toLowerCase();
        if (normalized.startsWith("m")) {
            return "M";
        }
        if (normalized.startsWith("f") || normalized.startsWith("z")) {
            return "F";
        }
        return GENDER_UNKNOWN;
    }

    @Override
    public String toDisplayGender(String rawGender) {
        String normalized = normalizeGender(rawGender);
        if ("M".equals(normalized)) {
            return Localization.get("profile.information.gender_male");
        }
        if ("F".equals(normalized)) {
            return Localization.get("profile.information.gender_female");
        }
        return "-";
    }
}

