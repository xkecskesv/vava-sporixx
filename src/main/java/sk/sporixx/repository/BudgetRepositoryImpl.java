package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Budget;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class BudgetRepositoryImpl implements BudgetRepository {

    private static final Logger logger = LoggerFactory.getLogger(BudgetRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private Budget mapResult(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getInt("id"));
        budget.setUserId(rs.getInt("user_id"));

        budget.setMonthlyIncome(rs.getDouble("monthly_income"));
        budget.setFood(rs.getDouble("food"));
        budget.setRent(rs.getDouble("rent"));
        budget.setTransport(rs.getDouble("transport"));
        budget.setUtilities(rs.getDouble("utilities"));
        budget.setOther(rs.getDouble("other"));

        budget.setEssentialExpenses(rs.getDouble("essential_expenses"));
        budget.setEmergencyFund(rs.getDouble("emergency_fund"));
        budget.setSavings(rs.getDouble("savings"));
        budget.setToInvest(rs.getDouble("to_invest"));
        budget.setFunMoney(rs.getDouble("fun_money"));

        budget.setMinimalEmergencyFund(rs.getDouble("minimal_emergency_fund"));
        budget.setOptimalEmergencyFund(rs.getDouble("optimal_emergency_fund"));

        budget.setActive(rs.getInt("is_active") == 1);

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            budget.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }

        return budget;
    }

    @Override
    public Optional<Budget> findByUserId(int userId) {
        String sql = "SELECT * FROM user_budgets WHERE user_id = ? AND is_active = 1 ORDER BY id DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding budget for user ID: {}", userId, e);
            throw new RuntimeException("Error reading budget from database", e);
        }
        return Optional.empty();
    }

    @Override
    public Budget save(Budget budget) {
        String sql = "INSERT INTO user_budgets (user_id, monthly_income, food, rent, transport, utilities, other, " +
                "essential_expenses, emergency_fund, savings, to_invest, fun_money, minimal_emergency_fund, optimal_emergency_fund, " +
                "period_type, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'tmp', ?, ?)"; //TODO: period_type neni zadefinovany v budget modeli

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, budget.getUserId());
            pstmt.setDouble(2, budget.getMonthlyIncome());
            pstmt.setDouble(3, budget.getFood());
            pstmt.setDouble(4, budget.getRent());
            pstmt.setDouble(5, budget.getTransport());
            pstmt.setDouble(6, budget.getUtilities());
            pstmt.setDouble(7, budget.getOther());
            pstmt.setDouble(8, budget.getEssentialExpenses());
            pstmt.setDouble(9, budget.getEmergencyFund());
            pstmt.setDouble(10, budget.getSavings());
            pstmt.setDouble(11, budget.getToInvest());
            pstmt.setDouble(12, budget.getFunMoney());
            pstmt.setDouble(13, budget.getMinimalEmergencyFund());
            pstmt.setDouble(14, budget.getOptimalEmergencyFund());
            pstmt.setInt(15, budget.isActive() ? 1 : 0);

            LocalDateTime createdAt = budget.getCreatedAt() != null ? budget.getCreatedAt() : LocalDateTime.now();
            pstmt.setString(16, createdAt.toString().replace("T", " "));

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        budget.setId(generatedKeys.getInt(1));
                        budget.setCreatedAt(createdAt);
                    }
                }
            }
            logger.info("New budget created for user ID: {}, Budget ID: {}", budget.getUserId(), budget.getId());

        } catch (SQLException e) {
            logger.error("Error saving budget for user ID: {}", budget.getUserId(), e);
            throw new RuntimeException("Error writing budget to database", e);
        }
        return budget;
    }

    @Override
    public void update(Budget budget) {
        String sql = "UPDATE user_budgets SET monthly_income = ?, food = ?, rent = ?, transport = ?, utilities = ?, other = ?, " +
                "essential_expenses = ?, emergency_fund = ?, savings = ?, to_invest = ?, fun_money = ?, " +
                "minimal_emergency_fund = ?, optimal_emergency_fund = ?, is_active = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, budget.getMonthlyIncome());
            pstmt.setDouble(2, budget.getFood());
            pstmt.setDouble(3, budget.getRent());
            pstmt.setDouble(4, budget.getTransport());
            pstmt.setDouble(5, budget.getUtilities());
            pstmt.setDouble(6, budget.getOther());
            pstmt.setDouble(7, budget.getEssentialExpenses());
            pstmt.setDouble(8, budget.getEmergencyFund());
            pstmt.setDouble(9, budget.getSavings());
            pstmt.setDouble(10, budget.getToInvest());
            pstmt.setDouble(11, budget.getFunMoney());
            pstmt.setDouble(12, budget.getMinimalEmergencyFund());
            pstmt.setDouble(13, budget.getOptimalEmergencyFund());
            pstmt.setInt(14, budget.isActive() ? 1 : 0);

            pstmt.setInt(15, budget.getId()); // WHERE podmienka

            pstmt.executeUpdate();
            logger.info("Budget updated. Budget ID: {}", budget.getId());

        } catch (SQLException e) {
            logger.error("Error updating budget ID: {}", budget.getId(), e);
            throw new RuntimeException("Error updating budget in database", e);
        }
    }
}