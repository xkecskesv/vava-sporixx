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
     * @param name meno
     * @param email email
     * @param password heslo
     * @param passwordConfirm potvrdenie hesla
     * @return vytvorený User s ID
     * @throws AuthException pri neúspešnej registrácii
     */
    User register(String name, String email, String password, String passwordConfirm) throws AuthException;

    /** Odhlási aktuálneho používateľa. */
    void logout();
}
