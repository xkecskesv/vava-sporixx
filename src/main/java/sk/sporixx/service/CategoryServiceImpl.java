package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Category;
import sk.sporixx.repository.CategoryRepository;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementácia CategoryService.
 * Spravuje systémové aj používateľské kategórie.
 * Systémové kategórie (userId = null) sa nedajú upravovať ani mazať.
 */
public class CategoryServiceImpl implements CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> getCategories() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading categories for userId: {}", userId);

        try {
            return categoryRepository.findByUserIdOrSystem(userId);
        } catch (Exception e) {
            logger.error("Failed to load categories for userId: {}", userId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    @Override
    public Category addCategory(String name) {
        logger.info("Adding category: {}", name);

        if (!ValidationUtil.isNotBlank(name)) {
            throw new CategoryException("category.error.name_required");
        }

        int userId = SessionManager.getInstance().getCurrentUserId();

        // Kontrola duplicity = case-insensitive
        List<Category> existing = categoryRepository.findByUserIdOrSystem(userId);
        boolean duplicate = existing.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()));

        if (duplicate) {
            logger.warn("Category already exists: {}", name);
            throw new CategoryException("category.error.already_exists");
        }

        try {
            Category category = Category.builder()
                    .userId(userId)
                    .name(name.trim())
                    .createdAt(LocalDateTime.now())
                    .build();

            Category saved = categoryRepository.save(category);
            logger.info("Category created: id={}, name={}", saved.getId(), saved.getName());
            return saved;

        } catch (Exception e) {
            logger.error("Failed to save category: {}", name, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    @Override
    public void updateCategory(int categoryId, String newName) {
        logger.info("Updating category id={} to name={}", categoryId, newName);

        if (!ValidationUtil.isNotBlank(newName)) {
            throw new CategoryException("category.error.name_required");
        }

        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new CategoryException("category.error.not_found");
        }

        Category category = categoryOpt.get();

        // Systémové kategórie sa nedajú upravovať
        if (category.isSystemCategory()) {
            logger.warn("Cannot update system category id={}", categoryId);
            throw new CategoryException("category.error.cannot_modify_system");
        }

        // Kontrola, že kategória patrí prihlásenému používateľovi
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (category.getUserId() != userId) {
            logger.warn("User {} tried to update category {} owned by {}",
                    userId, categoryId, category.getUserId());
            throw new CategoryException("category.error.not_found");
        }

        // Kontrola duplicity
        List<Category> existing = categoryRepository.findByUserIdOrSystem(userId);
        boolean duplicate = existing.stream()
                .filter(c -> c.getId() != categoryId)
                .anyMatch(c -> c.getName().equalsIgnoreCase(newName.trim()));

        if (duplicate) {
            throw new CategoryException("category.error.already_exists");
        }

        try {
            category.setName(newName.trim());
            categoryRepository.update(category);
            logger.info("Category updated: id={}, newName={}", categoryId, newName);

        } catch (Exception e) {
            logger.error("Failed to update category id={}", categoryId, e);
            throw new CategoryException("error.db_error", e);
        }
    }

    @Override
    public void deleteCategory(int categoryId) {
        logger.info("Deleting category id={}", categoryId);

        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new CategoryException("category.error.not_found");
        }

        Category category = categoryOpt.get();

        // Systémové kategórie sa nedajú mazať
        if (category.isSystemCategory()) {
            logger.warn("Cannot delete system category id={}", categoryId);
            throw new CategoryException("category.error.cannot_modify_system");
        }

        // Kontrola, že kategória patrí prihlásenému používateľovi
        int userId = SessionManager.getInstance().getCurrentUserId();
        if (category.getUserId() != userId) {
            logger.warn("User {} tried to delete category {} owned by {}",
                    userId, categoryId, category.getUserId());
            throw new CategoryException("category.error.not_found");
        }

        try {
            categoryRepository.deleteById(categoryId);
            logger.info("Category deleted: id={}", categoryId);

        } catch (Exception e) {
            logger.error("Failed to delete category id={}", categoryId, e);
            throw new CategoryException("error.db_error", e);
        }
    }
}