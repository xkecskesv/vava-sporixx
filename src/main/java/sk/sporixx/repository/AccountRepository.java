package sk.sporixx.repository;

import sk.sporixx.model.Account;

import java.util.List;
import java.util.Optional;

/**
 * Rozhranie pre prístup k dátam účtov.
 */
public interface AccountRepository {

    List<Account> findByOwnerUserId(int ownerUserId);

    Account save(Account account);
}
