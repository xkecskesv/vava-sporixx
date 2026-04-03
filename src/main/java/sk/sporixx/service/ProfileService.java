package sk.sporixx.service;

/**
 * Profile operations for the currently logged-in user.
 */
public interface ProfileService {

    /** Updates basic profile fields for current user. */
    void updateProfile(String firstName, String lastName, String email, String gender);

    /** Changes password for current user after validating old password. */
    void changePassword(String oldPassword, String newPassword);

    /** Persists profile photo path for the currently logged-in user. */
    void updateProfilePhoto(String photoPath);

    /** Converts raw/canonical gender value to localized UI value with fallback. */
    String toDisplayGender(String rawGender);
}

