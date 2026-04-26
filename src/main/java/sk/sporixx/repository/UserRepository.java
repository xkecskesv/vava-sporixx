package sk.sporixx.repository;

import sk.sporixx.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Rozhranie pre prístup k dátam používateľov.
 */
public interface UserRepository {

    /**
     * Nájde používateľa podľa emailu.
     *
     * @param email e-mailová adresa (už normalizovaná na lowercase)
     * @return Optional s používateľom, ak existuje, inak empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Nájde používateľa podľa ID.
     *
     * @param id primárny kľúč
     * @return Optional s používateľom, ak existuje
     */
    Optional<User> findById(int id);

    /**
     * Uloží nového používateľa do DB.
     * DÔLEŽITÉ: Po inserte nastaví vygenerované ID na User objekt.
     *
     * @param user objekt s vyplnenými údajmi (bez id)
     * @return User s nastaveným ID z DB
     */
    User save(User user);

    /**
     * Aktualizuje existujúceho používateľa.
     *
     * @param user objekt s nastaveným id a novými hodnotami
     * @return aktualizovaný používateľ
     */
    User update(User user);

    /**
     * Vráti všetkých používateľov pre admin prehľad.
     *
     * @return list všetkých používateľov
     */
    List<User> findAll();

    /**
     * Trvalo odstráni používateľa a všetky súvisiace záznamy podľa jeho ID.
     *
     * @param id identifikátor používateľa, ktorý sa má odstrániť
     */
    void deleteById(int id);

    /**
     * Synchronizuje oprávnenie roly rodinného manažéra v tabuľke {@code account_access}
     * pre zadaného používateľa.
     *
     * <p>Ak je {@code isFamilyManager} nastavené na {@code true}, repository zabezpečí,
     * aby mal používateľ aspoň jedno oprávnenie s úrovňou rodinného manažéra. Ak je
     * {@code false}, existujúce oprávnenia rodinného manažéra zníži na úroveň bežného
     * používateľa.</p>
     *
     * @param userId cieľové ID používateľa
     * @param isFamilyManager požadovaný stav roly rodinného manažéra
     */
    void updateFamilyManagerStatus(int userId, boolean isFamilyManager);
}