package sk.sporixx.service;

/**
 * Contract for profile operations performed for the currently authenticated user.
 *
 * @author Viktória Kecskés
 */
public interface ProfileService {

    /**
     * Updates profile identity fields.
     *
     * @param firstName new first name
     * @param lastName new last name
     * @param email new e-mail address
     * @param gender raw gender value from UI
     */
    void updateProfile(String firstName, String lastName, String email, String gender);

    /**
     * Changes password after validating current password and policy.
     *
     * @param oldPassword current plain-text password
     * @param newPassword new plain-text password
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * Persists selected profile photo path.
     *
     * @param photoPath absolute photo path
     */
    void updateProfilePhoto(String photoPath);

    /**
     * Converts raw/canonical gender value into localized UI text.
     *
     * @param rawGender raw persisted value
     * @return localized display value, or {@code "-"} fallback
     */
    String toDisplayGender(String rawGender);
}

