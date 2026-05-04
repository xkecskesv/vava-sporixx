package sk.sporixx.service.testovanie;

import sk.sporixx.model.User;
import sk.sporixx.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementácia UserRepository pre testovanie bez DB.
 * Dáta sa držia v List-e v pamäti - po reštarte aplikácie sa stratia.
 */
public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * Vyhľadá používateľa podľa e-mailu bez ohľadu na veľkosť písmen.
     *
     * @param email hľadaný e-mail
     * @return nájdený používateľ alebo prázdny výsledok
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    /**
     * Vyhľadá používateľa podľa identifikátora.
     *
     * @param id identifikátor používateľa
     * @return nájdený používateľ alebo prázdny výsledok
     */
    @Override
    public Optional<User> findById(int id) {
        return users.stream()
                .filter(u -> u.getId() == id)
                .findFirst();
    }

    /**
     * Uloží používateľa do in-memory kolekcie a podľa potreby priradí nové ID.
     *
     * @param user používateľ na uloženie
     * @return uložený používateľ
     */
    @Override
    public User save(User user) {
        if (user.getId() == 0) {
            user.setId(idGenerator.incrementAndGet());
        }
        users.add(user);
        return user;
    }

    @Override
    public User update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == user.getId()) {
                users.set(i, user);
                return user;
            }
        }
        throw new RuntimeException("User not found for update: id=" + user.getId());
    }

    /**
     * Pomocná metóda - vráti všetkých používateľov (pre debug/testovanie).
     */
    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public void deleteById(int id) {
        users.removeIf(user -> user.getId() == id);
    }

    /**
     * No-op implementácia pre in-memory testy.
     *
     * <p>Táto varianta repository nepracuje s tabuľkou {@code account_access}; stav
     * rodinného manažéra je počas testov riešený vo vyšších vrstvách cez session/rolu
     * používateľa.</p>
     *
     * @param userId cieľové ID používateľa
     * @param isFamilyManager požadovaný stav roly rodinného manažéra
     */
    @Override
    public void updateFamilyManagerStatus(int userId, boolean isFamilyManager) {
        // In-memory repository has no account_access model; profile tests rely on User.role in session.
    }

    @Override
    public void updateXpAndLevel(int userId, String category, double xp, int level) {
        users.stream()
                .filter(u -> u.getId() == userId)
                .findFirst()
                .ifPresent(u -> {
                    switch (category) {
                        case "saving" -> { u.setSavingXp(xp); u.setSavingLevel(level); }
                        case "budget" -> { u.setBudgetXp(xp); u.setBudgetLevel(level); }
                        case "investor" -> { u.setInvestorXp(xp); u.setInvestorLevel(level); }
                        case "spender" -> { u.setSpenderXp(xp); u.setSpenderLevel(level); }
                        default -> throw new IllegalArgumentException("Unknown category: " + category);
                    }
                });
    }
}