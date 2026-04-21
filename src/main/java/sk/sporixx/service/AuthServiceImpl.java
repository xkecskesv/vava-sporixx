package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.Account;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.UserRepository;
import sk.sporixx.util.DatabaseConfig;
import sk.sporixx.util.Localization;
import sk.sporixx.util.PasswordUtil;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Implementácia autentifikačnej služby.
 */
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private static final String DEFAULT_CURRENCY_CODE = "EUR";
    private static final int DEFAULT_REGION_ID = 1;
    private static final String ADMIN_EMAIL = "admin@sporixx.sk";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String ADMIN_FIRST_NAME = "Admin";
    private static final String ADMIN_LAST_NAME = "Sporixx";
    private static final int ADMIN_ACCESS_LEVEL = 3;

    public AuthServiceImpl(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        ensureAdminAccountExists();
    }

    //  LOGIN
    @Override
    public void login(String email, String password) throws AuthException {

        logger.info("Login attempt for email: {}", email);

        // Validácia vstupov
        if(!ValidationUtil.isNotBlank(email) && !ValidationUtil.isNotBlank(password)) {
            logger.warn("Login failed: empty email and password");
            throw new AuthException("auth.error.invalid_credentials");
        }
        if (!ValidationUtil.isNotBlank(email)) {
            logger.warn("Login failed: empty email");
            throw new AuthException("auth.error.email_required");
        }
        if (!ValidationUtil.isNotBlank(password)) {
            logger.warn("Login failed: empty password for email: {}", email);
            throw new AuthException("auth.error.password_required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Nájdenie používateľa v DB
        Optional<User> userOptional;
        try {
            userOptional = userRepository.findByEmail(normalizedEmail);
        } catch (Exception e) {
            logger.error("Database error during login for email: {}", normalizedEmail, e);
            throw new AuthException("auth.error.db_error", e);
        }

        if (userOptional.isEmpty()) {
            logger.warn("Login failed: no user found for email: {}", normalizedEmail);
            throw new AuthException("auth.error.invalid_credentials");
        }

        User user = userOptional.get();

        // Overenie hesla
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            logger.warn("Login failed: wrong password for email: {}", normalizedEmail);
            throw new AuthException("auth.error.invalid_credentials");
        }

        if (!user.isActive()) {
            logger.warn("Login failed: account inactive for email: {}", normalizedEmail);
            throw new AuthException("auth.error.account_inactive");
        }

        //  Načítanie účtov
        List<Account> accounts;
        try {
            accounts = accountRepository.findByOwnerUserId(user.getId());
        } catch (Exception e) {
            logger.error("Failed to load accounts for user: {}", user.getId(), e);
            throw new AuthException("auth.error.db_error", e);
        }

        // Nastavenie defaultnej roly
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        // Nastavenie session
        SessionManager.getInstance().setSession(user, accounts);
        boolean mustChangePassword = user.getRole() == Role.ADMIN && ADMIN_PASSWORD.equals(password);
        SessionManager.getInstance().setForcePasswordChange(mustChangePassword);

        logger.info("User logged in successfully: id={}, email={}, role={}, accounts={}",
                user.getId(), normalizedEmail, user.getRole(), accounts.size());
    }

    //  REGISTER
    @Override
    public void register(String firstName, String lastName, String email, String password, String passwordConfirm)
            throws AuthException {

        logger.info("Registration attempt for email: {}", email);

        // Validácia vstupov
        validateRegistrationInput(firstName, lastName, email, password, passwordConfirm);

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedFirst = ValidationUtil.normalizeName(firstName);
        String normalizedLast = ValidationUtil.normalizeName(lastName);

        // Kontrola duplicity emailu
        try {
            Optional<User> existing = userRepository.findByEmail(normalizedEmail);
            if (existing.isPresent()) {
                logger.warn("Registration failed: email already exists: {}", normalizedEmail);
                throw new AuthException("auth.error.email_exists");
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Database error during registration: {}", normalizedEmail, e);
            throw new AuthException("error.unexpected", e);
        }

        // Hashovanie hesla
        String passwordHash = PasswordUtil.hashPassword(password);

        // Vytvorenie a uloženie používateľa
        User user = User.builder()
                .firstName(normalizedFirst)
                .lastName(normalizedLast)
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .role(Role.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (Exception e) {
            logger.error("Failed to save user: {}", normalizedEmail, e);
            throw new AuthException("error.unexpected", e);
        }

        logger.info("User registered successfully: id={}, email={}", savedUser.getId(), normalizedEmail);

        // Vytvorenie defaultného Main Account and Emergency Fund
        try {
            Account mainAccount = Account.builder()
                    .ownerUserId(savedUser.getId())
                    .regionId(DEFAULT_REGION_ID)
                    .accountTypeId(Account.MAIN_ACCOUNT)
                    .defaultCurrencyCode(DEFAULT_CURRENCY_CODE)
                    .description(Localization.get("account.default.main_description"))
                    .initialBalance(0.0)
                    .currentBalance(0.0)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            Account emergencyFund = Account.builder()
                    .ownerUserId(savedUser.getId())
                    .regionId(DEFAULT_REGION_ID)
                    .accountTypeId(Account.EMERGENCY_FUND)
                    .defaultCurrencyCode(DEFAULT_CURRENCY_CODE)
                    .description(Localization.get("account.default.emergency_description"))
                    .initialBalance(0.0)
                    .currentBalance(0.0)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            accountRepository.save(mainAccount);
            accountRepository.save(emergencyFund);
            logger.info("Default accounts (Main Account, Emergency Fund) created for user: {}", savedUser.getId());
        } catch (Exception e) {
            logger.error("Failed to create default accounts for user: {}", savedUser.getId(), e);
        }

        // Auto-login po úspešnej registrácii
        try {
            List<Account> accounts = accountRepository.findByOwnerUserId(savedUser.getId());

            SessionManager.getInstance().setSession(savedUser, accounts);
            logger.info("Auto-login after registration: id={}, email={}", savedUser.getId(), normalizedEmail);
        } catch (Exception e) {
            logger.error("Auto-login failed after registration for user: {}", savedUser.getId(), e);
            // Registrácia prebehla úspešne, auto-login zlyhal – nevadí, používateľ sa prihlási manuálne
        }
    }

    //  LOGOUT
    @Override
    public void logout() {
        User currentUser = SessionManager.getInstance().getCurrentUserInternal();
        if (currentUser != null) {
            logger.info("User logging out: id={}, email={}", currentUser.getId(), currentUser.getEmail());
        }
        SessionManager.getInstance().clearSession();
        logger.info("Session cleared - user logged out");
    }

    /**
     * Validuje vstupy pri registrácii.
     * Poradie kontrol zodpovedá poradiu polí vo formulári (firstName, lastName, email, heslo, potvrdenie).
     */
    private void validateRegistrationInput(String firstName, String lastName, String email, String password, String passwordConfirm) throws AuthException {

        // firstName
        if (!ValidationUtil.isNotBlank(firstName)) {
            logger.warn("Registration validation: empty firstName");
            throw new AuthException("auth.error.first_name_required");
        }
        if (!ValidationUtil.isValidNamePartCharacters(firstName)) {
            logger.warn("Registration validation: invalid characters in firstName: {}", firstName);
            throw new AuthException("auth.error.invalid_first_name");
        }
        if (!ValidationUtil.isValidNamePart(firstName)) {
            logger.warn("Registration validation: firstName too many parts: {}", firstName);
            throw new AuthException("auth.error.first_name_too_long");
        }

        // lastName
        if (!ValidationUtil.isNotBlank(lastName)) {
            logger.warn("Registration validation: empty lastName");
            throw new AuthException("auth.error.last_name_required");
        }
        if (!ValidationUtil.isValidNamePartCharacters(lastName)) {
            logger.warn("Registration validation: invalid characters in lastName: {}", lastName);
            throw new AuthException("auth.error.invalid_last_name");
        }
        if (!ValidationUtil.isValidNamePart(lastName)) {
            logger.warn("Registration validation: lastName too many parts: {}", lastName);
            throw new AuthException("auth.error.last_name_too_long");
        }

        // Email
        if (!ValidationUtil.isNotBlank(email)) {
            logger.warn("Registration validation: empty email");
            throw new AuthException("auth.error.email_required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            logger.warn("Registration validation: invalid email: {}", email);
            throw new AuthException("auth.error.invalid_email");
        }

        // Heslo
        if (!ValidationUtil.isNotBlank(password)) {
            logger.warn("Registration validation: empty password");
            throw new AuthException("auth.error.password_required");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            logger.warn("Registration validation: password too short");
            throw new AuthException("auth.error.password_too_short");
        }

        // Zhoda hesiel
        if (!password.equals(passwordConfirm)) {
            logger.warn("Registration validation: passwords mismatch");
            throw new AuthException("auth.error.password_mismatch");
        }
    }

    /**
     * Ensures the constant admin account exists in persistence.
     */
    private void ensureAdminAccountExists() {
        try {
            Optional<User> existing = userRepository.findByEmail(ADMIN_EMAIL);
            if (existing.isPresent()) {
                ensureAdminHasActiveAccount(existing.get());
                ensureAdminAccessGrant(existing.get());
                return;
            }

            User admin = User.builder()
                    .firstName(ADMIN_FIRST_NAME)
                    .lastName(ADMIN_LAST_NAME)
                    .email(ADMIN_EMAIL)
                    .passwordHash(PasswordUtil.hashPassword(ADMIN_PASSWORD))
                    .gender(GenderCode.UNKNOWN)
                    .role(Role.ADMIN)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            User savedAdmin = userRepository.save(admin);

            ensureAdminHasActiveAccount(savedAdmin);
            ensureAdminAccessGrant(savedAdmin);
            logger.info("Default admin account created: {}", ADMIN_EMAIL);
        } catch (Exception e) {
            logger.error("Failed to ensure default admin account", e);
        }
    }

    private void ensureAdminHasActiveAccount(User adminUser) {
        List<Account> adminAccounts = accountRepository.findByOwnerUserId(adminUser.getId());
        boolean hasActive = adminAccounts.stream().anyMatch(Account::isActive);
        if (hasActive) {
            return;
        }

        Account adminMainAccount = Account.builder()
                .ownerUserId(adminUser.getId())
                .regionId(DEFAULT_REGION_ID)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .defaultCurrencyCode(DEFAULT_CURRENCY_CODE)
                .description("Admin account")
                .initialBalance(0.0)
                .currentBalance(0.0)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        accountRepository.save(adminMainAccount);
    }

    private void ensureAdminAccessGrant(User adminUser) {
        List<Account> adminAccounts = accountRepository.findByOwnerUserId(adminUser.getId());
        if (adminAccounts.isEmpty()) {
            return;
        }

        int accountId = adminAccounts.get(0).getId();
        String checkSql = "SELECT 1 FROM account_access WHERE user_id = ? AND account_id = ? AND access_level >= ?";
        String insertSql = "INSERT INTO account_access (user_id, account_id, access_level) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DatabaseConfig.SQLITE_URL);
             PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {

            checkStmt.setInt(1, adminUser.getId());
            checkStmt.setInt(2, accountId);
            checkStmt.setInt(3, ADMIN_ACCESS_LEVEL);

            try (ResultSet resultSet = checkStmt.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setInt(1, adminUser.getId());
                insertStmt.setInt(2, accountId);
                insertStmt.setInt(3, ADMIN_ACCESS_LEVEL);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to ensure admin access grant for user id={}", adminUser.getId(), e);
        }
    }
}