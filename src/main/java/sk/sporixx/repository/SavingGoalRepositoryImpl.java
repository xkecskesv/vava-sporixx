package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.SavingGoal;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SavingGoalRepositoryImpl implements SavingGoalRepository {

    private static final Logger logger = LoggerFactory.getLogger(SavingGoalRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private SavingGoal mapResult(ResultSet rs) throws SQLException {
        SavingGoal goal = new SavingGoal();

        goal.setId(rs.getInt("id"));
        goal.setAccountId(rs.getInt("account_id"));
        goal.setName(rs.getString("name"));
        goal.setGoalTypeId(rs.getInt("goal_type_id"));
        goal.setTargetAmount(rs.getDouble("target_amount"));
        goal.setCurrentAmount(rs.getDouble("current_amount"));
        goal.setActive(rs.getInt("is_active") == 1);

        String targetDateStr = rs.getString("target_date");
        if (targetDateStr != null) {
            goal.setTargetDate(LocalDateTime.parse(targetDateStr.replace(" ", "T")));
        }

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            goal.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }

        return goal;
    }

    @Override
    public List<SavingGoal> findActiveByAccountId(int accountId) {
        String sql = "SELECT * FROM saving_goals " +
                "WHERE account_id = ? AND is_active = 1 " +
                "ORDER BY created_at DESC";

        List<SavingGoal> goals = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                goals.add(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding active saving goals for accountId={}", accountId, e);
            throw new RuntimeException("Error reading saving goals (findActiveByAccountId)", e);
        }

        return goals;
    }

    @Override
    public List<SavingGoal> findActiveByAccountIds(List<Integer> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = accountIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT * FROM saving_goals " +
                "WHERE is_active = 1 AND account_id IN (" + placeholders + ") " +
                "ORDER BY created_at DESC";

        List<SavingGoal> goals = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < accountIds.size(); i++) {
                ps.setInt(i + 1, accountIds.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                goals.add(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding active saving goals for accountIds={}", accountIds, e);
            throw new RuntimeException("Error reading saving goals (findActiveByAccountIds)", e);
        }

        return goals;
    }

    @Override
    public Optional<SavingGoal> findById(int id) {
        String sql = "SELECT * FROM saving_goals WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding saving goal by id={}", id, e);
            throw new RuntimeException("Error reading saving goal (findById)", e);
        }

        return Optional.empty();
    }

    @Override
    public SavingGoal save(SavingGoal savingGoal) {
        String sql = "INSERT INTO saving_goals (" +
                "account_id, name, goal_type_id, target_amount, current_amount, target_date, is_active" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, savingGoal.getAccountId());
            ps.setString(2, savingGoal.getName());
            ps.setInt(3, savingGoal.getGoalTypeId());
            ps.setDouble(4, savingGoal.getTargetAmount());
            ps.setDouble(5, savingGoal.getCurrentAmount());

            if (savingGoal.getTargetDate() != null) {
                ps.setString(6, savingGoal.getTargetDate().toString().replace("T", " "));
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            ps.setInt(7, savingGoal.isActive() ? 1 : 0);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Creating saving goal failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    savingGoal.setId(generatedKeys.getInt(1));
                } else {
                    throw new RuntimeException("Creating saving goal failed, no generated ID returned.");
                }
            }

            if (savingGoal.getCreatedAt() == null) {
                savingGoal.setCreatedAt(LocalDateTime.now());
            }

            logger.info("Saving goal inserted with id={}", savingGoal.getId());

        } catch (SQLException e) {
            logger.error("Error saving saving goal for accountId={}", savingGoal.getAccountId(), e);
            throw new RuntimeException("Error writing saving goal (save)", e);
        }

        return savingGoal;
    }

    @Override
    public void updateCurrentAmount(int goalId, double currentAmount) {
        String sql = "UPDATE saving_goals SET current_amount = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, currentAmount);
            ps.setInt(2, goalId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No saving goal found to update current_amount, id=" + goalId);
            }

            logger.info("Saving goal current_amount updated. id={}, currentAmount={}", goalId, currentAmount);

        } catch (SQLException e) {
            logger.error("Error updating current_amount for saving goal id={}", goalId, e);
            throw new RuntimeException("Error updating saving goal current amount", e);
        }
    }

    @Override
    public void updateTargetAmount(int goalId, double targetAmount) {
        String sql = "UPDATE saving_goals SET target_amount = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, targetAmount);
            ps.setInt(2, goalId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No saving goal found to update target_amount, id=" + goalId);
            }

            logger.info("Saving goal target_amount updated. id={}, targetAmount={}", goalId, targetAmount);

        } catch (SQLException e) {
            logger.error("Error updating target_amount for saving goal id={}", goalId, e);
            throw new RuntimeException("Error updating saving goal target amount", e);
        }
    }

    @Override
    public void updateTargetDate(int goalId, LocalDateTime targetDate) {
        String sql = "UPDATE saving_goals SET target_date = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (targetDate != null) {
                ps.setString(1, targetDate.toString().replace("T", " "));
            } else {
                ps.setNull(1, Types.VARCHAR);
            }

            ps.setInt(2, goalId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No saving goal found to update target_date, id=" + goalId);
            }

            logger.info("Saving goal target_date updated. id={}, targetDate={}", goalId, targetDate);

        } catch (SQLException e) {
            logger.error("Error updating target_date for saving goal id={}", goalId, e);
            throw new RuntimeException("Error updating saving goal target date", e);
        }
    }
}