package sk.sporixx.service.testovanie;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.service.AuthService;
import sk.sporixx.service.AuthServiceImpl;
import sk.sporixx.util.PasswordUtil;

import java.time.LocalDateTime;

/**
 * Inicializátor testovacích dát pre service vrstvu.
 * Vytvorí predpripravených používateľov a ich účty v in-memory repozitároch,
 * takže nie je potrebná DB na otestovanie login/register tokov.
 * Použitie:
 *   TestDataInitializer init = new TestDataInitializer();
 *   AuthService authService = init.getAuthService();
 *   // Testovanie loginu s predpripraveným userom
 *   User user = authService.login("marek.mosko@stuba.sk", "Heslo123!");
 *   // Testovanie registrácie nového usera
 *   User newUser = authService.register("Anna Nová", "anna.nova@stuba.sk", "MojeHeslo1", "MojeHeslo1");
 * Prihlasovacie údaje predpripravených používateľov:
 *   BEŽNÝ POUŽÍVATEĽ:
 *     Email:    marek.mosko@stuba.sk
 *     Heslo:    Heslo123!
 *     Meno:     Marek Moško
 *     Účty:     Main Account (4 101.32 EUR)
 *               Emergency Fund (31 487.28 EUR)
 *               Saving Account - Porsche (88 667.93 EUR)
 *   ADMIN:
 *     Email:    admin@sporixx.sk
 *     Heslo:    Admin123!
 *     Meno:     Admin Sporixx
 *     Účty:     Main Account (0.00 EUR)
 *   RODIČ (FAMILY MANAGER):
 *     Email:    jana.mrkvickova@stuba.sk
 *     Heslo:    Rodic123!
 *     Meno:     Jana Mrkvičková
 *     Účty:     Main Account (12 500.00 EUR)
 *               Emergency Fund (5 000.00 EUR)
 */
@Getter
public class TestDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitializer.class);

    private final InMemoryUserRepository userRepository;
    private final InMemoryAccountRepository accountRepository;
    private final AuthService authService;

    private static final int REGION_SK = 1;

    public TestDataInitializer() {
        this.userRepository = new InMemoryUserRepository();
        this.accountRepository = new InMemoryAccountRepository();
        this.authService = new AuthServiceImpl(userRepository, accountRepository);

        initTestData();
    }

    /**
     * Vytvorí testovacích používateľov a ich účty.
     */
    private void initTestData() {
        logger.info("=== Inicializácia testovacích dát ===");

        createRegularUser();
        createAdminUser();
        createParentUser();

        logger.info("=== Testovacia dáta pripravené: {} users, {} accounts ===",
                userRepository.findAll().size(), accountRepository.findAll().size());
    }

    // ---- Bežný používateľ (podľa mockupov - Marek Moško) ----
    private void createRegularUser() {
        User user = User.builder()
                .firstName("Marek")
                .lastName("Moško")
                .email("marek.mosko@stuba.sk")
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .gender("M")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();

        User savedUser = userRepository.save(user);
        logger.info("Vytvorený testovací USER: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        // Main Account
        Account mainAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId())
                .regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .initialBalance(3_000.00)
                .currentBalance(4_101.32)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build());

        // Emergency Fund
        Account emergencyAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId())
                .regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND)
                .defaultCurrencyCode("EUR")
                .initialBalance(20_000.00)
                .currentBalance(31_487.28)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 1, 20, 14, 30))
                .build());

        // Saving Account
        Account savingAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedUser.getId())
                .regionId(REGION_SK)
                .accountTypeId(Account.SAVING_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .initialBalance(50_000.00)
                .currentBalance(88_667.93)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build());

        logger.info("  -> 3 účty vytvorené pre usera: {}", savedUser.getEmail());
    }

    // ---- Admin ----
    private void createAdminUser() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Sporixx")
                .email("admin@sporixx.sk")
                .passwordHash(PasswordUtil.hashPassword("Admin123!"))
                .gender("M")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        User savedAdmin = userRepository.save(admin);
        logger.info("Vytvorený testovací ADMIN: id={}, email={}", savedAdmin.getId(), savedAdmin.getEmail());

        Account mainAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedAdmin.getId())
                .regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .initialBalance(0.0)
                .currentBalance(0.0)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build());

        logger.info("  -> 1 účet vytvorený pre admina: {}", savedAdmin.getEmail());
    }

    // ---- Rodič / Family Manager ----
    private void createParentUser() {
        User parent = User.builder()
                .firstName("Jana")
                .lastName("Mrkvičková")
                .email("jana.mrkvickova@stuba.sk")
                .passwordHash(PasswordUtil.hashPassword("Rodic123!"))
                .gender("F")
                .role(Role.FAMILY_MANAGER)
                .createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build();

        User savedParent = userRepository.save(parent);
        logger.info("Vytvorený testovací PARENT: id={}, email={}", savedParent.getId(), savedParent.getEmail());

        // Main Account
        Account mainAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedParent.getId())
                .regionId(REGION_SK)
                .accountTypeId(Account.MAIN_ACCOUNT)
                .defaultCurrencyCode("EUR")
                .initialBalance(10_000.00)
                .currentBalance(12_500.00)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 2, 10, 8, 0))
                .build());

        // Emergency Fund
        Account emergencyAccount = accountRepository.save(Account.builder()
                .ownerUserId(savedParent.getId())
                .regionId(REGION_SK)
                .accountTypeId(Account.EMERGENCY_FUND)
                .defaultCurrencyCode("EUR")
                .initialBalance(5_000.00)
                .currentBalance(5_000.00)
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 2, 15, 12, 0))
                .build());

        logger.info("  -> 2 účty vytvorené pre rodiča: {}", savedParent.getEmail());
    }

}
