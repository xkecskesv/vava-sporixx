package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import sk.sporixx.model.*;
import sk.sporixx.repository.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Spoločný základ pre integration testy {@link ReportsServiceImpl} proti reálnej
 * JDBC SQLite databáze. Dedí z {@link JdbcTestSupport} ktorý zabezpečuje čistú DB
 * pre každý test.
 * <p>
 * Setup pripraví:
 *  <ul>
 *      <li>1 user (priamo v DB)</li>
 *      <li>1 main account + 1 saving account</li>
 *      <li>session manager nastavený na tohto usera</li>
 *      <li>reálne JDBC repository instancie</li>
 *  </ul>
 * <p>
 * Pozor: kategórie 1-9 sú už pred-naplnené v {@code test-schema.sql} (vrátane
 * systémových CATEGORY_SAVING=6 a CATEGORY_SAVING_EXPENSE=7).
 */
abstract class ReportsServiceJdbcTestSupport extends JdbcTestSupport {

    private static final DateTimeFormatter DB_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected TransactionRepository transactionRepo;
    protected RecurringRuleRepository recurringRepo;
    protected SavingGoalRepository savingGoalRepo;
    protected ReportsService reportsService;

    protected User testUser;
    protected Account mainAccount;
    protected Account savingAccount;

    @BeforeEach
    void reportsJdbcSetUp() throws SQLException {
        // 1. Vlož usera priamo do DB
        int userId = insertUser("test@sporixx.sk", "Test", "User", "F");

        testUser = User.builder()
                .id(userId)
                .email("test@sporixx.sk")
                .firstName("Test")
                .lastName("User")
                .role(Role.USER)
                .isActive(true)
                .build();

        // 2. Vlož 2 účty
        int mainId = insertAccount(userId, 1 /* MAIN */, 1000.0);
        int savingId = insertAccount(userId, 4 /* SAVING */, 500.0);

        mainAccount = Account.builder()
                .id(mainId)
                .ownerUserId(userId)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(1000.0)
                .currentBalance(1000.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();

        savingAccount = Account.builder()
                .id(savingId)
                .ownerUserId(userId)
                .accountTypeId(Account.SAVING_ACCOUNT)
                .regionId(1)
                .defaultCurrencyCode("EUR")
                .initialBalance(500.0)
                .currentBalance(500.0)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();

        // 3. Reálne JDBC repos (žiadne mocky)
        transactionRepo = new TransactionRepositoryImpl();
        recurringRepo = new RecurringRuleRepositoryImpl();
        savingGoalRepo = new SavingGoalRepositoryImpl();
        reportsService = new ReportsServiceImpl(transactionRepo, recurringRepo, savingGoalRepo);

        // 4. Session
        SessionManager.getInstance().setSession(testUser, List.of(mainAccount, savingAccount));
    }

    @AfterEach
    void reportsJdbcTearDown() {
        SessionManager.getInstance().clearSession();
    }

    // ====================== INSERT helpers ======================

    private int insertUser(String email, String firstName, String lastName, String gender)
            throws SQLException {
        String sql = "INSERT INTO users (email, password_hash, first_name, last_name, gender) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, email);
            pstmt.setString(2, "$2a$12$dummyhashfortestingpurposes000000000000000000000000000");
            pstmt.setString(3, firstName);
            pstmt.setString(4, lastName);
            pstmt.setString(5, gender);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to insert user");
    }

    private int insertAccount(int userId, int accountTypeId, double balance) throws SQLException {
        String sql = "INSERT INTO accounts (owner_user_id, region_id, account_type_id, " +
                "default_currency_code, initial_balance, current_balance, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1)";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, 1); // region SK
            pstmt.setString(3, String.valueOf(accountTypeId));
            pstmt.setString(4, "EUR");
            pstmt.setDouble(5, balance);
            pstmt.setDouble(6, balance);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to insert account");
    }

    /** Vlož transakciu priamo do DB (bypass service vrstvy aj repository.save). */
    protected int insertTransaction(int accountId, int typeId, int categoryId,
                                     Integer classification, double amount,
                                     LocalDateTime date) {
        String sql = "INSERT INTO transactions (account_id, name, category_id, " +
                "spending_classification_id, transaction_type_id, amount, status_id, " +
                "transaction_date) VALUES (?, ?, ?, ?, ?, ?, 1, ?)";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, "Test transaction");
            pstmt.setInt(3, categoryId);
            if (classification == null) {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(4, classification);
            }
            pstmt.setInt(5, typeId);
            pstmt.setDouble(6, amount);
            pstmt.setString(7, date.format(DB_FMT));
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction", e);
        }
        throw new RuntimeException("No generated key for transaction");
    }

    /** Vlož recurring rule priamo do DB. */
    protected int insertRecurringRule(int accountId, int categoryId, int classificationId,
                                       double amount, String description, boolean isActive) {
        String sql = "INSERT INTO recurring_rules (account_id, category_id, status_id, " +
                "transaction_type_id, spending_classification_id, amount, description, " +
                "frequency_type, frequency_interval, start_date, next_due_date, " +
                "is_active) VALUES (?, ?, 1, 2, ?, ?, ?, 'MONTHLY', 1, ?, ?, ?)";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, categoryId);
            pstmt.setInt(3, classificationId);
            pstmt.setDouble(4, amount);
            pstmt.setString(5, description);
            pstmt.setString(6, LocalDateTime.now().minusMonths(1).format(DB_FMT));
            pstmt.setString(7, LocalDateTime.now().plusDays(5).format(DB_FMT));
            pstmt.setInt(8, isActive ? 1 : 0);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert recurring rule", e);
        }
        throw new RuntimeException("No generated key for recurring rule");
    }

    /** Vlož saving goal priamo do DB. */
    protected int insertSavingGoal(int accountId, double target, double current,
                                    LocalDateTime created, LocalDateTime targetDate) {
        String sql = "INSERT INTO saving_goals (account_id, name, goal_type_id, " +
                "target_amount, current_amount, target_date, is_active, created_at) " +
                "VALUES (?, 'Test goal', 1, ?, ?, ?, 1, ?)";
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, accountId);
            pstmt.setDouble(2, target);
            pstmt.setDouble(3, current);
            pstmt.setString(4, targetDate.format(DB_FMT));
            pstmt.setString(5, created.format(DB_FMT));
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert saving goal", e);
        }
        throw new RuntimeException("No generated key for saving goal");
    }

    /** Spočíta riadky v transactions tabuľke (sanity check). */
    protected int countTransactions() {
        try (Connection conn = openConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
