package sk.sporixx.service.testovanie;

import sk.sporixx.model.Account;
import sk.sporixx.repository.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * In-memory implementácia AccountRepository pre testovanie bez DB.
 * Dáta sa držia v List-e v pamäti - po reštarte aplikácie sa stratia.
 */
public class InMemoryAccountRepository implements AccountRepository {

    private final List<Account> accounts = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * Vráti všetky aktívne účty patriace zadanému používateľovi.
     *
     * @param userId identifikátor vlastníka účtov
     * @return zoznam aktívnych účtov používateľa
     */
    @Override
    public List<Account> findByOwnerUserId(int userId) {
        return accounts.stream()
                .filter(a -> a.getOwnerUserId() == userId)
                .filter(Account::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> findAllByOwnerUserId(int userId) {
        return accounts.stream()
                .filter(a -> a.getOwnerUserId() == userId)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Account> findById(int accountId) {
        return accounts.stream()
                .filter(a -> a.getId() == accountId)
                .findFirst();
    }

    /**
     * Uloží účet do in-memory kolekcie a podľa potreby priradí nové ID.
     *
     * @param account účet na uloženie
     * @return uložený účet
     */
    @Override
    public Account save(Account account) {
        if (account.getId() == 0) {
            account.setId(idGenerator.incrementAndGet());
        }
        accounts.add(account);
        return account;
    }

    @Override
    public void updateBalance(int accountId, double newBalance) {
        accounts.stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .ifPresent(a -> a.setCurrentBalance(newBalance));
    }

    @Override
    public void update(Account account) {
        accounts.stream()
                .filter(a -> a.getId() == account.getId())
                .findFirst()
                .ifPresent(a -> a.setDescription(account.getDescription()));
    }

    @Override
    public void deactivateById(int accountId) {
        accounts.stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .ifPresent(a -> a.setActive(false));
    }

    @Override
    public void activateById(int accountId) {
        accounts.stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .ifPresent(a -> a.setActive(true));
    }

    /**
     * Pomocná metóda - vráti všetky účty (pre debug/testovanie).
     */
    public List<Account> findAll() {
        return new ArrayList<>(accounts);
    }
}
