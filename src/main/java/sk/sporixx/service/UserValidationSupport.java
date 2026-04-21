package sk.sporixx.service;

import sk.sporixx.model.User;
import sk.sporixx.util.PasswordUtil;
import sk.sporixx.util.ValidationUtil;

/**
 * Zdieľané validačné pomocné metódy pre identitu používateľa a pravidlá hesla.
 */
public final class UserValidationSupport {

    private UserValidationSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validuje základné identifikačné údaje používateľa.
     *
     * @param firstName meno používateľa
     * @param lastName priezvisko používateľa
     * @param email e-mailová adresa používateľa
     */
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

    /**
     * Overí podmienky pre zmenu hesla aktuálne prihláseného používateľa.
     *
     * @param oldPassword pôvodné heslo
     * @param newPassword nové heslo
     * @param currentUser aktuálne prihlásený používateľ
     */
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

    /**
     * Aplikuje zmenu hesla na používateľský objekt uložením nového hashu.
     *
     * @param newPassword nové heslo v otvorenom texte
     * @param currentUser používateľ, ktorému sa mení heslo
     */
    public static void applyPasswordChange(String newPassword, User currentUser) {
        currentUser.setPasswordHash(PasswordUtil.hashPassword(newPassword));
    }
}

