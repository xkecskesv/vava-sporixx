package sk.sporixx.service;

import sk.sporixx.model.User;
import sk.sporixx.util.PasswordUtil;
import sk.sporixx.util.ValidationUtil;

/**
 * Shared validation helpers for user identity and password rules.
 */
public final class UserValidationSupport {

    private UserValidationSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void validateIdentity(String firstName, String lastName, String email) {
        if (!ValidationUtil.isNotBlank(firstName)) {
            throw new ProfileException("auth.error.first_name_required");
        }
        if (!ValidationUtil.isNotBlank(lastName)) {
            throw new ProfileException("auth.error.last_name_required");
        }
        if (!ValidationUtil.isValidNamePart(firstName) || !ValidationUtil.isValidNamePartCharacters(firstName)) {
            throw new ProfileException("auth.error.invalid_first_name");
        }
        if (!ValidationUtil.isValidNamePart(lastName) || !ValidationUtil.isValidNamePartCharacters(lastName)) {
            throw new ProfileException("auth.error.invalid_last_name");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ProfileException("auth.error.invalid_email");
        }
    }

    public static void validatePasswordChange(String oldPassword, String newPassword, User currentUser) {
        if (!ValidationUtil.isNotBlank(oldPassword)) {
            throw new ProfileException("auth.error.old_password_required");
        }
        if (!PasswordUtil.verifyPassword(oldPassword, currentUser.getPasswordHash())) {
            throw new ProfileException("auth.error.wrong_old_password");
        }
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new ProfileException("auth.error.password_too_short");
        }
        if (PasswordUtil.verifyPassword(newPassword, currentUser.getPasswordHash())) {
            throw new ProfileException("auth.error.same_password");
        }
    }

    public static void applyPasswordChange(String newPassword, User currentUser) {
        currentUser.setPasswordHash(PasswordUtil.hashPassword(newPassword));
    }
}

