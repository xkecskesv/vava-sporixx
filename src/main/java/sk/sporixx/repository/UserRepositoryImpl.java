package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ukladanie a hladanie usera z db
 */
public class UserRepositoryImpl implements UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // Mapuje dáta z databázy na Java objekt
    private User mapResult(ResultSet result) throws SQLException {
        User user = new User();
        user.setId(result.getInt("id"));
        user.setEmail(result.getString("email"));
        user.setPasswordHash(result.getString("password_hash"));
        user.setFirstName(result.getString("first_name"));
        user.setLastName(result.getString("last_name"));
        user.setPhotoPath(result.getString("photo_path"));
        user.setGender(result.getString("gender"));

        String createdAtStr = result.getString("created_at");
        if (createdAtStr != null) {
            user.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }
        return user;
    }

    //TODO: vo find nastaviť role usera


    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by email: {}", email, e);
            throw new RuntimeException("Error reading from database (findByEmail)", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by ID: {}", id, e);
            throw new RuntimeException("Error reading from database (findById)", e);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, photo_path, gender, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFirstName());
            pstmt.setString(4, user.getLastName());
            pstmt.setString(5, user.getPhotoPath());

            String gender = user.getGender() != null ? user.getGender() : "ONHSR";
            pstmt.setString(6, gender);

            LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
            pstmt.setString(7, createdAt.toString().replace("T", " "));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                        user.setCreatedAt(createdAt);
                    }
                }
            }
            logger.info("User successfully saved to DB with ID: {}", user.getId());

        } catch (SQLException e) {
            logger.error("Database error while saving user with email: {}", user.getEmail(), e);
            throw new RuntimeException("Error writing to database (save)", e);
        }
        return user;
    }
}