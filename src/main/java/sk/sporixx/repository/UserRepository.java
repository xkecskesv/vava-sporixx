package sk.sporixx.repository;

import sk.sporixx.model.User;

import java.util.List;

public interface UserRepository {

    boolean save(User user);

    boolean update(User user);

    boolean deleteById(Long id);

    User findById(Long id);

    User findByUsername(String username);

    List<User> findAll();
}