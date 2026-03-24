package sk.sporixx.service;

import sk.sporixx.model.User;

/**
 * Rozhranie pre autentifikáciu.
 */
public interface AuthService {
    /**
     * Prihlási používateľa - overí email + heslo, nastaví session.
     * @param email emailová adresa
     * @param password heslo v čistom texte
     * @return prihlásený User
     * @throws AuthException pri neúspešnom prihlásení
     */
    User login(String email, String password) throws AuthException;

    /**
     * Zaregistruje nového používateľa a vytvorí mu defaultný Main Account.
     * @param user údaje nového používateľa
     * @param password heslo
     * @param passwordConfirm potvrdenie hesla
     * @return vytvorený User s ID
     * @throws AuthException pri neúspešnej registrácii
     */
    User register(User user, String password, String passwordConfirm) throws AuthException;

    /** Odhlási aktuálneho používateľa. */
    void logout();

    /**
     * Zmení heslo používateľa.
     * @param userId ID používateľa
     * @param oldPassword staré heslo (na overenie)
     * @param newPassword nové heslo
     * @throws AuthException ak staré heslo nesedí alebo nové je slabé
     */
    void changePassword(int userId, String oldPassword, String newPassword) throws AuthException;
}
