package sk.sporixx.repository;

import sk.sporixx.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Rozhranie pre prístup k dátam používateľov.
 */
public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(int id);

    User save(User user);
}