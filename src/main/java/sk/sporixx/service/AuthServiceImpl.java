package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.UserRepository;
import sk.sporixx.util.PasswordUtil;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementácia autentifikačnej služby.
 */
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private static final String DEFAULT_ACCOUNT_NAME = "Main Account";
    private static final String DEFAULT_CURRENCY_CODE = "EUR";
    private static final int DEFAULT_REGION_ID = 1;

    public AuthServiceImpl(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    //  LOGIN
    @Override
    public User login(String email, String password) throws AuthException {

        logger.info("Login attempt for email: {}", email);

        // Validácia vstupov
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
        //TODO: dokoncit ked bude hotova backend vrstva
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

        //  Načítanie účtov
        //TODO
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

        logger.info("User logged in successfully: id={}, email={}, role={}, accounts={}",
                user.getId(), normalizedEmail, user.getRole(), accounts.size());

        return user;
    }

    //  REGISTER
    @Override
    public User register(String name, String email, String password, String passwordConfirm)
            throws AuthException {

        logger.info("Registration attempt for email: {}", email);

        // Validácia vstupov
        validateRegistrationInput(name, email, password, passwordConfirm);

        String normalizedEmail = email.trim().toLowerCase();
        String trimmedName = name.trim().replaceAll("\\s+", " ");

        // Kontrola duplicity emailu
        //TODO
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
            throw new AuthException("auth.error.db_error", e);
        }

        // Hashovanie hesla
        String passwordHash = PasswordUtil.hashPassword(password);

        // Vytvorenie a uloženie používateľa
        User user = User.builder()
                .name(trimmedName)
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser;
        //TODO
        try {
            savedUser = userRepository.save(user);
        } catch (Exception e) {
            logger.error("Failed to save user: {}", normalizedEmail, e);
            throw new AuthException("auth.error.db_error", e);
        }

        logger.info("User registered successfully: id={}, email={}",
                savedUser.getId(), normalizedEmail);

        // Vytvorenie defaultného Main Account
        try {
            Account mainAccount = Account.builder()
                    .ownerUserId(savedUser.getId())
                    .regionId(DEFAULT_REGION_ID)
                    .accountName(DEFAULT_ACCOUNT_NAME)
                    .defaultCurrencyCode(DEFAULT_CURRENCY_CODE)
                    .initialBalance(0.0)
                    .currentBalance(0.0)
                    .createdAt(LocalDateTime.now())
                    .build();

            accountRepository.save(mainAccount);
            logger.info("Default Main Account created for user: {}", savedUser.getId());
        } catch (Exception e) {
            logger.error("Failed to create default account for user: {}", savedUser.getId(), e);
        }

        return savedUser;
    }

    //  LOGOUT
    @Override
    public void logout() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            logger.info("User logging out: id={}, email={}", currentUser.getId(), currentUser.getEmail());
        }
        SessionManager.getInstance().clearSession();
        logger.info("Session cleared - user logged out");
    }

    //  PRIVÁTNA VALIDÁCIA (DRY)
    /**
     * Validuje vstupy pri registrácii.
     * Poradie kontrol zodpovedá poradiu polí vo formulári (meno, email, heslo, potvrdenie).
     */
    private void validateRegistrationInput(String name, String email, String password, String passwordConfirm) throws AuthException {

        // Meno (celé meno v jednom poli)
        if (!ValidationUtil.isNotBlank(name)) {
            logger.warn("Registration validation: empty name");
            throw new AuthException("auth.error.name_required");
        }
        if (!ValidationUtil.isValidFullName(name)) {
            logger.warn("Registration validation: invalid name: {}", name);
            throw new AuthException("auth.error.invalid_name");
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

        // Zhoda hesiel
        if (!password.equals(passwordConfirm)) {
            logger.warn("Registration validation: passwords mismatch");
            throw new AuthException("auth.error.password_mismatch");
        }
    }
}