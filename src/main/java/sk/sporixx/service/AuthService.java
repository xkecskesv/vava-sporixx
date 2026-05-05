package sk.sporixx.service;

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
     * Zaregistruje nového používateľa a vytvorí mu defaultné účty.
     * @param firstName meno (max 2 slová)
     * @param lastName  priezvisko (max 2 slová)
     * @param email email
     * @param password heslo
     * @param passwordConfirm potvrdenie hesla
     * @throws AuthException pri neúspešnej registrácii
     */
    void register(String firstName, String lastName, String email, String password, String passwordConfirm) throws AuthException;

    /** Odhlási aktuálneho používateľa. */
    void logout();
}
