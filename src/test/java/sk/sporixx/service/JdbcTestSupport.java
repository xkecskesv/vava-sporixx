package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Spoločný základ pre INTEGRATION testy proti REÁLNEJ JDBC SQLite vrstve.
 * <p>
 * Tieto testy NIE SÚ unit testy — používajú reálne {@code TransactionRepositoryImpl},
 * {@code SavingGoalRepositoryImpl}, atď., ktoré komunikujú so SQLite DB cez JDBC.
 * Cieľom je odhaliť bugy v SQL dotazoch, mapovaní stĺpcov, FK obmedzeniach,
 * a iných veciach ktoré in-memory unit testy nezachytia.
 * <p>
 * Stratégia:
 * <ol>
 *   <li>Repositories majú {@code DB_URL = "jdbc:sqlite:sporixx.sqlite"} — hardcoded.
 *       Preto pred každým testom <b>prepíšeme tento súbor</b> v pracovnom adresári
 *       čerstvou testovacou DB (zo {@code test-schema.sql}).</li>
 *   <li>Ak v adresári existoval produkčný {@code sporixx.sqlite}, najprv ho zálohujeme
 *       a po teste obnovíme — aby developerova databáza nebola pri spúšťaní testov zničená.</li>
 *   <li>Po teste DB súbor zmažeme.</li>
 * </ol>
 * <p>
 * <b>Poznámka:</b> hardcoded DB_URL je samotný anti-pattern. Refactor návrh:
 * inject {@code String dbUrl} do konštruktora repository, alebo zaviesť centrálnu
 * {@code DataSource} ktorú mockujeme. Toto je out-of-scope tester roly, ale
 * stojí za komunikáciu s tímom.
 */
abstract class JdbcTestSupport {

    private static final Path DB_FILE = Paths.get("sporixx.sqlite");
    private static final Path BACKUP_FILE = Paths.get("sporixx.sqlite.testbackup");
    private static final String SCHEMA_RESOURCE = "/test-schema.sql";

    @BeforeEach
    void jdbcSetUp() throws IOException, SQLException {
        // 1. Zálohuj produkčnú DB ak existuje
        if (Files.exists(DB_FILE)) {
            Files.copy(DB_FILE, BACKUP_FILE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.delete(DB_FILE);
        }

        // 2. Vytvor čerstvú testovaciu DB s našou schémou
        String schema = loadSchema();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
             Statement stmt = conn.createStatement()) {
            // Spusti všetky CREATE / INSERT statementy
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    @AfterEach
    void jdbcTearDown() throws IOException {
        // 1. Zmaž testovaciu DB
        Files.deleteIfExists(DB_FILE);

        // 2. Obnov pôvodnú produkčnú DB
        if (Files.exists(BACKUP_FILE)) {
            Files.move(BACKUP_FILE, DB_FILE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String loadSchema() throws IOException {
        try (InputStream is = JdbcTestSupport.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new IOException("Test schema not found at " + SCHEMA_RESOURCE);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ============ Helpers ============

    /** Otvor connection na rovnakú DB ktorú používajú repositories. */
    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
    }

    /** Vykoná SQL — vhodné na priame INSERT/UPDATE v setupe testov. */
    protected void execSql(String sql) {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to exec: " + sql, e);
        }
    }
}
