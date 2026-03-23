package sk.sporixx.repository;

import sk.sporixx.model.Category;

import java.util.List;

public interface CategoryRepository {

    boolean save(Category category);

    boolean update(Category category);

    boolean deleteById(Long id);

    Category findById(Long id);

    List<Category> findAll();

    List<Category> search(String query);
}