package sk.sporixx.repository;

import sk.sporixx.model.AccountAccess;
import java.util.List;

public interface AccountAccessRepository {

    /**
     * Nájde všetky prístupy pre daného používateľa.
     * Používa sa na načítanie účtov detí pre Family Managera.
     */
    List<AccountAccess> findByUserId(int userId);

    /**
     * Udelí prístup k účtu.
     */
    void grantAccess(int userId, int accountId, int accessLevel);

    /**
     * Zruší prístup k jednému účtu.
     */
    void revokeAccess(int userId, int accountId);

    /**
     * Zruší všetky prístupy daného používateľa.
     * Používa sa pri odstránení dieťaťa z rodiny.
     */
    void revokeAllAccess(int userId);

    List<AccountAccess> findByAccountId(int accountId);

    /**
     * Zruší všetky prístupy k danému účtu.
     * Volá sa pri deaktivácii účtu.
     */
    void revokeAllAccessForAccount(int accountId);
}