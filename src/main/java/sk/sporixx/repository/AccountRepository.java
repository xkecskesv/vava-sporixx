package sk.sporixx.repository;

import sk.sporixx.model.Account;

import java.util.List;
import java.util.Optional;

/**
 * Rozhranie pre prístup k dátam účtov.
 */
public interface AccountRepository {

    /**
     * Nájde všetky účty používateľa.
     * SQL: SELECT * FROM accounts WHERE user_id = ?
     *
     * @param ownerUserId ID vlastníka učtov
     * @return zoznam všetkých učtov používateľa (Main, Emergency, Saving...)
     */
    List<Account> findByOwnerUserId(int ownerUserId);

    /**
     * Nájde účet podľa ID.
     */
    Optional<Account> findById(int accountId);

    /**
     * Vráti súčet zostatkov všetkých aktívnych účtov používateľa.
     */
    double sumBalanceByOwnerUserId(int userId);

    /**
     * Uloží nový účet do DB.
     * DOLEZITE: Po inserte nastaviť vygenerovane ID na Account objekt.
     *
     * @param account objekt s vyplnenými údajmi (bez id)
     * @return Account s nastaveným ID z DB
     */
    Account save(Account account);

    /**
     * Aktualizuje zostatok na účte.
     */
    void updateBalance(int accountId, double newBalance);
}
