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
 * Nahradiť JDBC implementáciou keď bude hotová DB vrstva.
 */
public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Optional<User> findById(int id) {
        return users.stream()
                .filter(u -> u.getId() == id)
                .findFirst();
    }

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

    @Override
    public void updateFamilyManagerStatus(int userId, boolean isFamilyManager) {
        // In-memory repository has no account_access model; profile tests rely on User.role in session.
    }
}