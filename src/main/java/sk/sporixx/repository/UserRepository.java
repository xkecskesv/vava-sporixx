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
     * @param email emailova adresa (uz normalizovana na lowercase)
     * @return Optional s User ak existuje, inak empty
     */
    Optional<User> findByEmail(String email);

    /**
     * Nájde používateľa podla ID.
     *
     * @param id primárny kľúč
     * @return Optional s User ak existuje
     */
    Optional<User> findById(int id);

    /**
     * Uloží nového používateľa do DB.
     * DOLEZITE: Po inserte nastavit vygenerovane ID na User objekt
     *
     * @param user objekt s vyplnenými údajmi (bez id)
     * @return User s nastavenym ID z DB
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
}