package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Category;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepositoryImpl implements CategoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(CategoryRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    @Override
    public List<Category> findByUserIdOrSystem(int userId) {
        String sql = "SELECT id, user_id, name, created_at " +
                "FROM categories " +
                "WHERE user_id IS NULL OR user_id = ? " +
                "ORDER BY name ASC";

        List<Category> categories = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                Category category = new Category();
                category.setId(result.getInt("id"));
                category.setUserId((Integer) result.getObject("user_id"));
                category.setName(result.getString("name"));

                String createdAtStr = result.getString("created_at");
                if (createdAtStr != null) {
                    category.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                }

                categories.add(category);
            }

            logger.debug("Found {} categories (system + user={})", categories.size(), userId);

        } catch (SQLException e) {
            logger.error("Error finding categories for userId={} including system categories", userId, e);
            throw new RuntimeException("Error reading categories from database (findByUserIdOrSystem)", e);
        }

        return categories;
    }

    @Override
    public Optional<Category> findById(int categoryId) {
        String sql = "SELECT id, user_id, name, created_at FROM categories WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);
            ResultSet result = pstmt.executeQuery();

            if (result.next()) {
                Category category = new Category();
                category.setId(result.getInt("id"));
                category.setUserId((Integer) result.getObject("user_id"));
                category.setName(result.getString("name"));

                String createdAtStr = result.getString("created_at");
                if (createdAtStr != null) {
                    category.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
                }

                return Optional.of(category);
            }

        } catch (SQLException e) {
            logger.error("Error finding category by ID={}", categoryId, e);
            throw new RuntimeException("Error reading category from database (findById)", e);
        }

        return Optional.empty();
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories (user_id, name) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (category.getUserId() != null) {
                pstmt.setInt(1, category.getUserId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }

            pstmt.setString(2, category.getName());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Creating category failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    category.setId(generatedKeys.getInt(1));
                } else {
                    throw new RuntimeException("Creating category failed, no generated ID returned.");
                }
            }

            if (category.getCreatedAt() == null) {
                category.setCreatedAt(LocalDateTime.now());
            }

            logger.info("Category saved with ID: {}", category.getId());

        } catch (SQLException e) {
            logger.error("Error saving category name='{}', userId={}", category.getName(), category.getUserId(), e);
            throw new RuntimeException("Error writing category to database (save)", e);
        }

        return category;
    }

    @Override
    public void deleteById(int categoryId) {
        String sql = "DELETE FROM categories WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No category found to delete with ID: " + categoryId);
            }

            logger.info("Category deleted. ID: {}", categoryId);

        } catch (SQLException e) {
            logger.error("Error deleting category ID={}", categoryId, e);
            throw new RuntimeException("Error deleting category from database (deleteById)", e);
        }
    }

    @Override
    public void update(Category category) {
        // TODO: implementovať — UPDATE categories SET name = ? WHERE id = ?
        throw new UnsupportedOperationException("Not implemented yet");
    }
}