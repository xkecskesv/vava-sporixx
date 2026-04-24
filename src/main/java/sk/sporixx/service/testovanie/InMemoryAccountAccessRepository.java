package sk.sporixx.service.testovanie;

import sk.sporixx.model.AccountAccess;
import sk.sporixx.repository.AccountAccessRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryAccountAccessRepository implements AccountAccessRepository {

    private final List<AccountAccess> accesses = new ArrayList<>();

    @Override
    public List<AccountAccess> findByUserId(int userId) {
        return accesses.stream()
                .filter(a -> a.getUserId() == userId)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountAccess> findByAccountId(int accountId) {
        return accesses.stream()
                .filter(a -> a.getAccountId() == accountId)
                .collect(Collectors.toList());
    }

    @Override
    public void grantAccess(int userId, int accountId, int accessLevel) {
        // Skontroluj či prístup už existuje
        boolean exists = accesses.stream()
                .anyMatch(a -> a.getUserId() == userId
                        && a.getAccountId() == accountId);
        if (exists) return;

        accesses.add(AccountAccess.builder()
                .userId(userId)
                .accountId(accountId)
                .accessLevel(accessLevel)
                .grantedAt(LocalDateTime.now())
                .build());
    }

    @Override
    public void revokeAccess(int userId, int accountId) {
        accesses.removeIf(a -> a.getUserId() == userId
                && a.getAccountId() == accountId);
    }

    @Override
    public void revokeAllAccess(int userId) {
        accesses.removeIf(a -> a.getUserId() == userId);
    }

    public List<AccountAccess> findAll() {
        return new ArrayList<>(accesses);
    }

    @Override
    public void revokeAllAccessForAccount(int accountId) {
        accesses.removeIf(a -> a.getAccountId() == accountId);
    }
}
