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
     * @throws AuthException pri neúspešnom prihlásení
     */
    void login(String email, String password) throws AuthException;

    /**
     * Zaregistruje nového používateľa a vytvorí mu defaultný Main Account.
     * @param name meno
     * @param email email
     * @param password heslo
     * @param passwordConfirm potvrdenie hesla
     * @throws AuthException pri neúspešnej registrácii
     */
    void register(String name, String email, String password, String passwordConfirm) throws AuthException;

    /** Odhlási aktuálneho používateľa. */
    void logout();
}
