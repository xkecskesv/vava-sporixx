package sk.sporixx.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JDBC implementácia pre TransactionRepository.
 */
public class TransactionRepositoryImpl implements TransactionRepository {

    private static final Logger logger = LoggerFactory.getLogger(TransactionRepositoryImpl.class);
    private static final String DB_URL = "jdbc:sqlite:sporixx.sqlite";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Mapovanie transaction do db.
     * Poznámka - db má stĺpec "name", model má field "description".
     */
    private Transaction mapResult(ResultSet results) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(results.getInt("id"));
        transaction.setAccountId(results.getInt("account_id"));
        transaction.setCategoryId(results.getInt("category_id"));
        transaction.setTransactionTypeId(results.getInt("transaction_type_id"));
        transaction.setTransactionStatusId(results.getInt("status_id"));
        transaction.setSpendingClassificationId((Integer) results.getObject("spending_classification_id"));
        transaction.setAmount(results.getDouble("amount"));
        transaction.setDescription(results.getString("name"));

        String completeDateStr = results.getString("transaction_date");
        if (completeDateStr != null) {
            transaction.setCompleteDate(LocalDateTime.parse(completeDateStr.replace(" ", "T")));
        }

        String createdAtStr = results.getString("created_at");
        if (createdAtStr != null) {
            transaction.setCreatedAt(LocalDateTime.parse(createdAtStr.replace(" ", "T")));
        }

        transaction.setTargetAccountId(null);
        transaction.setCurrencyCode(null);

        return transaction;
    }

    @Override
    public Optional<Transaction> findById(int id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet result = pstmt.executeQuery();

            if (result.next()) {
                return Optional.of(mapResult(result));
            }

        } catch (SQLException e) {
            logger.error("Error finding transaction by ID: {}", id, e);
            throw new RuntimeException("Error reading transaction from database (findById)", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Transaction> findByAccountIdAndDateRange(int accountId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT * FROM transactions " +
                "WHERE account_id = ? AND transaction_date >= ? AND transaction_date <= ? " +
                "ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            pstmt.setString(2, from.toString().replace("T", " "));
            pstmt.setString(3, to.toString().replace("T", " "));

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapResult(rs));
            }

            logger.info("Found {} transactions for account {} in range {} - {}",
                    transactions.size(), accountId, from, to);

        } catch (SQLException e) {
            logger.error("Error finding transactions for account {} in range {} - {}", accountId, from, to, e);
            throw new RuntimeException("Error reading transactions from database (findByAccountIdAndDateRange)", e);
        }

        return transactions;
    }

    @Override
    public Map<String, Double> sumByTypeAndMonth(int accountId, int transactionTypeId, LocalDateTime from) {
        String sql = "SELECT strftime('%Y-%m', transaction_date) AS period, SUM(amount) AS total " +
                "FROM transactions " +
                "WHERE account_id = ? AND transaction_type_id = ? AND transaction_date >= ? " +
                "GROUP BY strftime('%Y-%m', transaction_date) " +
                "ORDER BY period ASC";

        Map<String, Double> sumsByMonth = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            pstmt.setInt(2, transactionTypeId);
            pstmt.setString(3, from.toString().replace("T", " "));

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByMonth.put(result.getString("period"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing monthly transactions for account {}, type {}, from {}",
                    accountId, transactionTypeId, from, e);
            throw new RuntimeException("Error summarizing transactions by month", e);
        }
        return sumsByMonth;
    }

    @Override
    public Map<String, Double> sumByTypeAndDay(int accountId, int transactionTypeId, LocalDateTime from) {
        String sql = "SELECT strftime('%Y-%m-%d', transaction_date) AS period, SUM(amount) AS total " +
                "FROM transactions " +
                "WHERE account_id = ? AND transaction_type_id = ? AND transaction_date >= ? " +
                "GROUP BY strftime('%Y-%m-%d', transaction_date) " +
                "ORDER BY period ASC";

        Map<String, Double> sumsByDay = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            pstmt.setInt(2, transactionTypeId);
            pstmt.setString(3, from.toString().replace("T", " "));

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByDay.put(result.getString("period"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing daily transactions for account {}, type {}, from {}",
                    accountId, transactionTypeId, from, e);
            throw new RuntimeException("Error summarizing transactions by day", e);
        }
        return sumsByDay;
    }

    @Override
    public Map<String, Double> sumByCategoryAndDateRange(int accountId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT c.name AS category_name, SUM(t.amount) AS total " +
                "FROM transactions t " +
                "JOIN categories c ON c.id = t.category_id " +
                "WHERE t.account_id = ? " +
                "AND t.transaction_date >= ? " +
                "AND t.transaction_date <= ? " +
                "AND t.transaction_type_id = ? " +
                "GROUP BY c.name " +
                "ORDER BY total DESC";

        Map<String, Double> sumsByCategory = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            pstmt.setString(2, from.toString().replace("T", " "));
            pstmt.setString(3, to.toString().replace("T", " "));
            pstmt.setInt(4, Transaction.TYPE_EXPENSE);

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByCategory.put(result.getString("category_name"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing category expenses for account {} in range {} - {}",
                    accountId, from, to, e);
            throw new RuntimeException("Error summarizing expenses by category", e);
        }

        return sumsByCategory;
    }

    @Override
    public Map<String, Double> sumByClassificationAndDateRange(int accountId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT sc.name AS classification_name, SUM(t.amount) AS total " +
                "FROM transactions t " +
                "JOIN spending_classification sc ON sc.id = t.spending_classification_id " +
                "WHERE t.account_id = ? " +
                "AND t.transaction_date >= ? " +
                "AND t.transaction_date <= ? " +
                "AND t.transaction_type_id = ? " +
                "AND t.spending_classification_id IS NOT NULL " +
                "GROUP BY sc.name " +
                "ORDER BY total DESC";

        Map<String, Double> sumsByClassification = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            pstmt.setString(2, from.toString().replace("T", " "));
            pstmt.setString(3, to.toString().replace("T", " "));
            pstmt.setInt(4, Transaction.TYPE_EXPENSE);

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByClassification.put(result.getString("classification_name"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing expenses by classification for account {} in range {} - {}",
                    accountId, from, to, e);
            throw new RuntimeException("Error summarizing expenses by classification", e);
        }

        return sumsByClassification;
    }

    @Override
    public Transaction save(Transaction transaction) {
        String sql = "INSERT INTO transactions (" +
                "account_id, name, category_id, spending_classification_id, transaction_type_id, amount, status_id, transaction_date" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, transaction.getAccountId());
            pstmt.setString(2, transaction.getDescription());
            pstmt.setInt(3, transaction.getCategoryId());

            if (transaction.getSpendingClassificationId() != null) {
                pstmt.setInt(4, transaction.getSpendingClassificationId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }

            pstmt.setInt(5, transaction.getTransactionTypeId());
            pstmt.setDouble(6, transaction.getAmount());
            pstmt.setInt(7, transaction.getTransactionStatusId());

            LocalDateTime txDate = transaction.getCompleteDate() != null
                    ? transaction.getCompleteDate()
                    : LocalDateTime.now();
            pstmt.setString(8, txDate.toString().replace("T", " "));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Creating transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getInt(1));
                } else {
                    throw new RuntimeException("Creating transaction failed, no generated ID returned.");
                }
            }

            transaction.setCompleteDate(txDate);
            if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(LocalDateTime.now());
            }

            logger.info("Transaction saved with ID: {}", transaction.getId());

        } catch (SQLException e) {
            logger.error("Error saving transaction for account {}", transaction.getAccountId(), e);
            throw new RuntimeException("Error writing transaction to database (save)", e);
        }

        return transaction;
    }

    @Override
    public void saveTransfer(Transaction expense, Transaction income,
                             int fromAccountId, double fromNewBalance,
                             int toAccountId, double toNewBalance) {
        String insertSql = "INSERT INTO transactions " +
                "(account_id, name, category_id, spending_classification_id, transaction_type_id, amount, status_id, transaction_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateBalanceSql = "UPDATE accounts SET current_balance = ? WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertTransactionInConn(conn, insertSql, expense);
                insertTransactionInConn(conn, insertSql, income);

                try (PreparedStatement ps = conn.prepareStatement(updateBalanceSql)) {
                    ps.setDouble(1, fromNewBalance);
                    ps.setInt(2, fromAccountId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(updateBalanceSql)) {
                    ps.setDouble(1, toNewBalance);
                    ps.setInt(2, toAccountId);
                    ps.executeUpdate();
                }

                conn.commit();
                logger.info("Transfer saved atomically: expense id={}, income id={}", expense.getId(), income.getId());
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Transfer rolled back due to error", e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving transfer atomically", e);
        }
    }

    private void insertTransactionInConn(Connection conn, String sql, Transaction tx) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, tx.getAccountId());
            pstmt.setString(2, tx.getDescription());
            pstmt.setInt(3, tx.getCategoryId());
            if (tx.getSpendingClassificationId() != null) {
                pstmt.setInt(4, tx.getSpendingClassificationId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setInt(5, tx.getTransactionTypeId());
            pstmt.setDouble(6, Math.round(tx.getAmount() * 100.0) / 100.0);
            pstmt.setInt(7, tx.getTransactionStatusId());
            LocalDateTime txDate = tx.getCompleteDate() != null ? tx.getCompleteDate() : LocalDateTime.now();
            pstmt.setString(8, txDate.toString().replace("T", " "));
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) tx.setId(keys.getInt(1));
            }
            tx.setCompleteDate(txDate);
            if (tx.getCreatedAt() == null) tx.setCreatedAt(LocalDateTime.now());
        }
    }

    @Override
    public void update(Transaction transaction) {
        String sql = "UPDATE transactions SET " +
                "account_id = ?, " +
                "name = ?, " +
                "category_id = ?, " +
                "spending_classification_id = ?, " +
                "transaction_type_id = ?, " +
                "amount = ?, " +
                "status_id = ?, " +
                "transaction_date = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transaction.getAccountId());
            pstmt.setString(2, transaction.getDescription());
            pstmt.setInt(3, transaction.getCategoryId());

            if (transaction.getSpendingClassificationId() != null) {
                pstmt.setInt(4, transaction.getSpendingClassificationId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }

            pstmt.setInt(5, transaction.getTransactionTypeId());
            pstmt.setDouble(6, transaction.getAmount());
            pstmt.setInt(7, transaction.getTransactionStatusId());

            LocalDateTime txDate = transaction.getCompleteDate() != null
                    ? transaction.getCompleteDate()
                    : LocalDateTime.now();
            pstmt.setString(8, txDate.toString().replace("T", " "));
            pstmt.setInt(9, transaction.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No transaction found to update with ID: " + transaction.getId());
            }

            logger.info("Transaction updated. ID: {}", transaction.getId());

        } catch (SQLException e) {
            logger.error("Error updating transaction ID: {}", transaction.getId(), e);
            throw new RuntimeException("Error updating transaction in database", e);
        }
    }

    @Override
    public void deleteById(int id) {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No transaction found to delete with ID: " + id);
            }

            logger.info("Transaction deleted. ID: {}", id);

        } catch (SQLException e) {
            logger.error("Error deleting transaction ID: {}", id, e);
            throw new RuntimeException("Error deleting transaction from database", e);
        }
    }

    @Override
    public List<Transaction> findByAccountId(int accountId) {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_date DESC";
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding transactions for account {}", accountId, e);
            throw new RuntimeException("Error reading transactions by account ID", e);
        }
        return transactions;
    }

    @Override
    public Map<String, Double> sumByTypeAndMonth(int accountId, int transactionTypeId,
                                                 LocalDateTime from,
                                                 List<Integer> excludeCategoryIds) {
        StringBuilder sql = new StringBuilder(
                "SELECT strftime('%Y-%m', transaction_date) AS period, SUM(amount) AS total " +
                        "FROM transactions " +
                        "WHERE account_id = ? AND transaction_type_id = ? AND transaction_date >= ? "
        );

        boolean hasExclusions = excludeCategoryIds != null && !excludeCategoryIds.isEmpty();
        if (hasExclusions) {
            String placeholders = String.join(",", Collections.nCopies(excludeCategoryIds.size(), "?"));
            sql.append("AND (category_id NOT IN (").append(placeholders).append(") OR category_id IS NULL) ");
        }

        sql.append("GROUP BY strftime('%Y-%m', transaction_date) ORDER BY period ASC");
        Map<String, Double> sumsByMonth = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setInt(1, accountId);
            pstmt.setInt(2, transactionTypeId);
            pstmt.setString(3, from.toString().replace("T", " "));

            int paramIndex = 4;
            if (hasExclusions) {
                for (Integer catId : excludeCategoryIds) {
                    pstmt.setInt(paramIndex++, catId);
                }
            }

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByMonth.put(result.getString("period"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing monthly transactions with exclusions", e);
            throw new RuntimeException("Error summarizing transactions by month", e);
        }
        return sumsByMonth;
    }

    @Override
    public Map<String, Double> sumByTypeAndDay(int accountId, int transactionTypeId,
                                               LocalDateTime from,
                                               List<Integer> excludeCategoryIds) {
        StringBuilder sql = new StringBuilder(
                "SELECT strftime('%Y-%m-%d', transaction_date) AS period, SUM(amount) AS total " +
                        "FROM transactions " +
                        "WHERE account_id = ? AND transaction_type_id = ? AND transaction_date >= ? "
        );

        boolean hasExclusions = excludeCategoryIds != null && !excludeCategoryIds.isEmpty();
        if (hasExclusions) {
            String placeholders = String.join(",", Collections.nCopies(excludeCategoryIds.size(), "?"));
            sql.append("AND (category_id NOT IN (").append(placeholders).append(") OR category_id IS NULL) ");
        }

        sql.append("GROUP BY strftime('%Y-%m-%d', transaction_date) ORDER BY period ASC");
        Map<String, Double> sumsByDay = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setInt(1, accountId);
            pstmt.setInt(2, transactionTypeId);
            pstmt.setString(3, from.toString().replace("T", " "));

            int paramIndex = 4;
            if (hasExclusions) {
                for (Integer catId : excludeCategoryIds) {
                    pstmt.setInt(paramIndex++, catId);
                }
            }

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByDay.put(result.getString("period"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing daily transactions with exclusions", e);
            throw new RuntimeException("Error summarizing transactions by day", e);
        }
        return sumsByDay;
    }

    @Override
    public Map<String, Double> sumByCategoryAndDateRange(int accountId, LocalDateTime from,
                                                         LocalDateTime to,
                                                         List<Integer> excludeCategoryIds) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.name AS category_name, SUM(t.amount) AS total " +
                        "FROM transactions t " +
                        "JOIN categories c ON c.id = t.category_id " +
                        "WHERE t.account_id = ? " +
                        "AND t.transaction_date >= ? " +
                        "AND t.transaction_date <= ? " +
                        "AND t.transaction_type_id = ? "
        );

        boolean hasExclusions = excludeCategoryIds != null && !excludeCategoryIds.isEmpty();
        if (hasExclusions) {
            String placeholders = String.join(",", Collections.nCopies(excludeCategoryIds.size(), "?"));
            sql.append("AND t.category_id NOT IN (").append(placeholders).append(") ");
        }

        sql.append("GROUP BY c.name ORDER BY total DESC");
        Map<String, Double> sumsByCategory = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setInt(1, accountId);
            pstmt.setString(2, from.toString().replace("T", " "));
            pstmt.setString(3, to.toString().replace("T", " "));
            pstmt.setInt(4, Transaction.TYPE_EXPENSE);

            int paramIndex = 5;
            if (hasExclusions) {
                for (Integer catId : excludeCategoryIds) {
                    pstmt.setInt(paramIndex++, catId);
                }
            }

            ResultSet result = pstmt.executeQuery();
            while (result.next()) {
                sumsByCategory.put(result.getString("category_name"), result.getDouble("total"));
            }

        } catch (SQLException e) {
            logger.error("Error summarizing category expenses with exclusions", e);
            throw new RuntimeException("Error summarizing expenses by category", e);
        }
        return sumsByCategory;
    }

    @Override
    public List<Transaction> findByFilters(int accountId, Integer categoryId,
                                           LocalDateTime dateFrom, LocalDateTime dateTo,
                                           Double amountFrom, Double amountTo,
                                           Integer transactionTypeId) {
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE account_id = ? ");
        params.add(accountId);

        if (categoryId != null) {
            sql.append("AND category_id = ? ");
            params.add(categoryId);
        }
        if (dateFrom != null) {
            sql.append("AND transaction_date >= ? ");
            params.add(dateFrom.toString().replace("T", " "));
        }
        if (dateTo != null) {
            sql.append("AND transaction_date <= ? ");
            params.add(dateTo.toString().replace("T", " "));
        }
        if (amountFrom != null) {
            sql.append("AND amount >= ? ");
            params.add(amountFrom);
        }
        if (amountTo != null) {
            sql.append("AND amount <= ? ");
            params.add(amountTo);
        }
        if (transactionTypeId != null) {
            sql.append("AND transaction_type_id = ? ");
            params.add(transactionTypeId);
        }

        sql.append("ORDER BY transaction_date DESC");
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                transactions.add(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error filtering transactions", e);
            throw new RuntimeException("Error applying transaction filters", e);
        }
        return transactions;
    }

    @Override
    public Optional<Transaction> findPairedTransfer(int excludeAccountId,
                                                    double amount,
                                                    LocalDateTime createdAt) {
        String sql = "SELECT * FROM transactions " +
                "WHERE account_id != ? " +
                "AND amount = ? " +
                "AND created_at = ? " +
                "AND category_id IN (?, ?, ?) " +
                "LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, excludeAccountId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, createdAt.toString().replace("T", " "));
            pstmt.setInt(4, Transaction.CATEGORY_SAVING);
            pstmt.setInt(5, Transaction.CATEGORY_SAVING_EXPENSE);
            pstmt.setInt(6, Transaction.CATEGORY_TRANSFER);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResult(rs));
            }

        } catch (SQLException e) {
            logger.error("Error finding paired transfer for accountId={}, amount={}, createdAt={}",
                    excludeAccountId, amount, createdAt, e);
            throw new RuntimeException("Error finding paired transfer", e);
        }

        return Optional.empty();
    }

    @Override
    public boolean existsByCategoryId(int categoryId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE category_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking if category id={} is used in transactions", categoryId, e);
            throw new RuntimeException("Error checking category usage in transactions", e);
        }

        return false;
    }
}