package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.FamilyRequest;
import sk.sporixx.util.DatabaseConfig;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FamilyRequestRepositoryImpl implements FamilyRequestRepository {

    private static final Logger logger = LoggerFactory.getLogger(FamilyRequestRepositoryImpl.class);

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
    }

    private FamilyRequest mapResult(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        if (rs.getString("created_at") != null) {
            createdAt = LocalDateTime.parse(rs.getString("created_at").replace(" ", "T"));
        }

        return FamilyRequest.builder()
                .id(rs.getInt("id"))
                .fromUserId(rs.getInt("from_user_id"))
                .toUserId(rs.getInt("to_user_id"))
                .status(rs.getString("status"))
                .createdAt(createdAt)
                .build();
    }

    @Override
    public FamilyRequest save(FamilyRequest request) {
        String sql = "INSERT INTO family_requests (from_user_id, to_user_id, status, created_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, request.getFromUserId());
            pstmt.setInt(2, request.getToUserId());

            String status = request.getStatus() != null ? request.getStatus() : FamilyRequest.STATUS_PENDING;
            pstmt.setString(3, status);

            LocalDateTime createdAt = request.getCreatedAt() != null ? request.getCreatedAt() : LocalDateTime.now();
            pstmt.setString(4, createdAt.toString().replace("T", " "));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        request.setId(generatedKeys.getInt(1));
                        request.setStatus(status);
                        request.setCreatedAt(createdAt);
                    }
                }
            }
            logger.info("Created new family request from user {} to user {}, status: {}",
                    request.getFromUserId(), request.getToUserId(), status);

        } catch (SQLException e) {
            logger.error("Error saving family request from user {} to user {}", request.getFromUserId(), request.getToUserId(), e);
            throw new RuntimeException("Error writing family request to database", e);
        }
        return request;
    }

    @Override
    public Optional<FamilyRequest> findById(int id) {
        String sql = "SELECT * FROM family_requests WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding family request by ID: {}", id, e);
            throw new RuntimeException("Error reading family request from database", e);
        }
        return Optional.empty();
    }

    @Override
    public List<FamilyRequest> findPendingByToUserId(int toUserId) {
        String sql = "SELECT * FROM family_requests WHERE to_user_id = ? AND status = ?";
        List<FamilyRequest> requests = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, toUserId);
            pstmt.setString(2, FamilyRequest.STATUS_PENDING);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(mapResult(rs));
            }
            logger.debug("Found {} pending requests for toUserId: {}", requests.size(), toUserId);
        } catch (SQLException e) {
            logger.error("Error finding pending requests for toUserId: {}", toUserId, e);
            throw new RuntimeException("Error reading family requests from database", e);
        }
        return requests;
    }

    @Override
    public boolean existsPending(int fromUserId, int toUserId) {
        String sql = "SELECT 1 FROM family_requests WHERE from_user_id = ? AND to_user_id = ? AND status = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, fromUserId);
            pstmt.setInt(2, toUserId);
            pstmt.setString(3, FamilyRequest.STATUS_PENDING);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.error("Error checking pending request existence between user {} and {}", fromUserId, toUserId, e);
            throw new RuntimeException("Error reading family requests from database", e);
        }
    }

    @Override
    public void updateStatus(int requestId, String status) {
        String sql = "UPDATE family_requests SET status = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No family request found to update with ID: " + requestId);
            }

            logger.info("Updated family request ID {} to status {}", requestId, status);

        } catch (SQLException e) {
            logger.error("Error updating status for request ID: {}", requestId, e);
            throw new RuntimeException("Error updating family request status in database", e);
        }
    }

    @Override
    public List<FamilyRequest> findPendingByFromUserId(int fromUserId) {
        String sql = "SELECT * FROM family_requests WHERE from_user_id = ? AND status = ?";
        List<FamilyRequest> requests = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, fromUserId);
            pstmt.setString(2, FamilyRequest.STATUS_PENDING);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(mapResult(rs));
            }
            logger.debug("Found {} pending requests for fromUserId: {}", requests.size(), fromUserId);
        } catch (SQLException e) {
            logger.error("Error finding pending requests for fromUserId: {}", fromUserId, e);
            throw new RuntimeException("Error reading family requests from database", e);
        }
        return requests;
    }
}