package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.RecurringRule;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecurringRuleRepositoryImpl implements RecurringRuleRepository {

    private static final Logger logger = LoggerFactory.getLogger(RecurringRuleRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private RecurringRule mapResult(ResultSet rs) throws SQLException {
        RecurringRule rule = new RecurringRule();

        rule.setId(rs.getInt("id"));
        rule.setAccountId(rs.getInt("account_id"));
        rule.setCategoryId(rs.getInt("category_id"));
        rule.setStatusId(rs.getInt("status_id"));
        rule.setTransactionTypeId(rs.getInt("transaction_type_id"));

        Object sc = rs.getObject("spending_classification_id");
        rule.setSpendingClassificationId(sc != null ? ((Number) sc).intValue() : 0);

        rule.setAmount(rs.getDouble("amount"));
        rule.setDescription(rs.getString("description"));
        rule.setFrequencyType(rs.getString("frequency_type"));
        rule.setFrequencyInterval(rs.getInt("frequency_interval"));

        String startDate = rs.getString("start_date");
        if (startDate != null) rule.setStartDate(LocalDateTime.parse(startDate.replace(" ", "T")));

        String nextDueDate = rs.getString("next_due_date");
        if (nextDueDate != null) rule.setNextDueDate(LocalDateTime.parse(nextDueDate.replace(" ", "T")));

        String endDate = rs.getString("end_date");
        if (endDate != null) rule.setEndDate(LocalDateTime.parse(endDate.replace(" ", "T")));

        Object maxOcc = rs.getObject("max_occurrences");
        rule.setMaxOccurrences(maxOcc != null ? ((Number) maxOcc).intValue() : null);

        rule.setGeneratedCount(rs.getInt("generated_count"));
        rule.setActive(rs.getInt("is_active") == 1);

        String createdAt = rs.getString("created_at");
        if (createdAt != null) rule.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));

        return rule;
    }

    @Override
    public List<RecurringRule> findActiveByAccountId(int accountId) {
        String sql = "SELECT * FROM recurring_rules " +
                "WHERE account_id = ? AND is_active = 1 " +
                "ORDER BY next_due_date ASC";

        List<RecurringRule> rules = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rules.add(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding active recurring rules for accountId={}", accountId, e);
            throw new RuntimeException("Error reading recurring rules (findActiveByAccountId)", e);
        }

        return rules;
    }

    @Override
    public List<RecurringRule> findUpcomingByAccountId(int accountId, LocalDateTime now, int limit) {
        String sql = "SELECT * FROM recurring_rules " +
                "WHERE account_id = ? " +
                "AND is_active = 1 " +
                "AND next_due_date >= ? " +
                "ORDER BY next_due_date ASC " +
                "LIMIT ?";

        List<RecurringRule> rules = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, accountId);
            ps.setString(2, now.toString().replace("T", " "));
            ps.setInt(3, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rules.add(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding upcoming recurring rules for accountId={}, now={}, limit={}", accountId, now, limit, e);
            throw new RuntimeException("Error reading recurring rules (findUpcomingByAccountId)", e);
        }

        return rules;
    }

    @Override
    public RecurringRule save(RecurringRule rule) {
        if (rule.getId() <= 0) {
            return insert(rule);
        } else {
            return update(rule);
        }
    }

    private RecurringRule insert(RecurringRule rule) {
        String sql = "INSERT INTO recurring_rules (" +
                "account_id, category_id, status_id, transaction_type_id, spending_classification_id, " +
                "amount, description, frequency_type, frequency_interval, start_date, next_due_date, end_date, " +
                "max_occurrences, generated_count, is_active" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, rule.getAccountId());
            ps.setInt(2, rule.getCategoryId());
            ps.setInt(3, rule.getStatusId());
            ps.setInt(4, rule.getTransactionTypeId());

            if (rule.getSpendingClassificationId() > 0) ps.setInt(5, rule.getSpendingClassificationId());
            else ps.setNull(5, Types.INTEGER);

            ps.setDouble(6, rule.getAmount());
            ps.setString(7, rule.getDescription());
            ps.setString(8, rule.getFrequencyType());
            ps.setInt(9, rule.getFrequencyInterval());

            ps.setString(10, rule.getStartDate().toString().replace("T", " "));
            ps.setString(11, rule.getNextDueDate().toString().replace("T", " "));

            if (rule.getEndDate() != null) ps.setString(12, rule.getEndDate().toString().replace("T", " "));
            else ps.setNull(12, Types.VARCHAR);

            if (rule.getMaxOccurrences() != null) ps.setInt(13, rule.getMaxOccurrences());
            else ps.setNull(13, Types.INTEGER);

            ps.setInt(14, rule.getGeneratedCount());
            ps.setInt(15, rule.isActive() ? 1 : 0);

            int affected = ps.executeUpdate();
            if (affected == 0) throw new RuntimeException("Creating recurring rule failed, no rows affected.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    rule.setId(keys.getInt(1));
                } else {
                    throw new RuntimeException("Creating recurring rule failed, no generated ID.");
                }
            }

            logger.info("Recurring rule inserted with ID={}", rule.getId());

        } catch (SQLException e) {
            logger.error("Error inserting recurring rule for accountId={}", rule.getAccountId(), e);
            throw new RuntimeException("Error writing recurring rule (insert)", e);
        }

        return rule;
    }

    private RecurringRule update(RecurringRule rule) {
        String sql = "UPDATE recurring_rules SET " +
                "account_id = ?, category_id = ?, status_id = ?, transaction_type_id = ?, spending_classification_id = ?, " +
                "amount = ?, description = ?, frequency_type = ?, frequency_interval = ?, start_date = ?, " +
                "next_due_date = ?, end_date = ?, max_occurrences = ?, generated_count = ?, is_active = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, rule.getAccountId());
            ps.setInt(2, rule.getCategoryId());
            ps.setInt(3, rule.getStatusId());
            ps.setInt(4, rule.getTransactionTypeId());

            if (rule.getSpendingClassificationId() > 0) ps.setInt(5, rule.getSpendingClassificationId());
            else ps.setNull(5, Types.INTEGER);

            ps.setDouble(6, rule.getAmount());
            ps.setString(7, rule.getDescription());
            ps.setString(8, rule.getFrequencyType());
            ps.setInt(9, rule.getFrequencyInterval());
            ps.setString(10, rule.getStartDate().toString().replace("T", " "));
            ps.setString(11, rule.getNextDueDate().toString().replace("T", " "));

            if (rule.getEndDate() != null) ps.setString(12, rule.getEndDate().toString().replace("T", " "));
            else ps.setNull(12, Types.VARCHAR);

            if (rule.getMaxOccurrences() != null) ps.setInt(13, rule.getMaxOccurrences());
            else ps.setNull(13, Types.INTEGER);

            ps.setInt(14, rule.getGeneratedCount());
            ps.setInt(15, rule.isActive() ? 1 : 0);
            ps.setInt(16, rule.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) throw new RuntimeException("No recurring rule found to update with ID: " + rule.getId());

            logger.info("Recurring rule updated. ID={}", rule.getId());

        } catch (SQLException e) {
            logger.error("Error updating recurring rule ID={}", rule.getId(), e);
            throw new RuntimeException("Error writing recurring rule (update)", e);
        }

        return rule;
    }

    @Override
    public void updateNextDueDate(int ruleId, LocalDateTime nextDueDate, int generatedCount) {
        String sql = "UPDATE recurring_rules SET next_due_date = ?, generated_count = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (nextDueDate != null) ps.setString(1, nextDueDate.toString().replace("T", " "));
            else ps.setNull(1, Types.VARCHAR);

            ps.setInt(2, generatedCount);
            ps.setInt(3, ruleId);

            int affected = ps.executeUpdate();
            if (affected == 0) throw new RuntimeException("No recurring rule found to update with ID: " + ruleId);

            logger.info("Recurring rule nextDueDate updated. ID={}, generatedCount={}", ruleId, generatedCount);

        } catch (SQLException e) {
            logger.error("Error updating next due date for recurring rule ID={}", ruleId, e);
            throw new RuntimeException("Error updating recurring rule next due date", e);
        }
    }

    @Override
    public Optional<RecurringRule> findById(int id) {
        // TODO: SELECT * FROM recurring_rules WHERE id = ?
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deactivateById(int id) {
        // TODO: UPDATE recurring_rules SET is_active = 0 WHERE id = ?
        throw new UnsupportedOperationException("Not implemented yet");
    }
}