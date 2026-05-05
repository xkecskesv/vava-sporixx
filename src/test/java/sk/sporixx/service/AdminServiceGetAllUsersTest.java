package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.GenderCode;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.repository.UserRepository;
import sk.sporixx.service.testovanie.InMemoryAccountAccessRepository;
import sk.sporixx.service.testovanie.InMemoryUserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link AdminService#getAllUsers()}.
 * Overujeme:
 *   - autorizáciu (iba admin)
 *   - správnosť DTO mapovania (admin flag, family flag, name concat, null-safe trim)
 *   - prázdny zoznam
 *   - failing repository
 */
class AdminServiceGetAllUsersTest extends AdminServiceTestSupport {

    // ====================== HAPPY PATH ======================

    @Test
    @DisplayName("getAllUsers vráti všetkých používateľov ako DTO (admin je prihlásený)")
    void getAllUsers_returnsAllUsersAsDto() {
        // v baseSetUp sme vytvorili 3 users (admin, user, family manager)
        List<AdminUserData> users = adminService.getAllUsers();

        assertEquals(3, users.size(), "Mali by sme vidieť všetkých troch používateľov");
    }

    @Test
    @DisplayName("DTO mapping – admin flag sa nastavuje podľa Role.ADMIN")
    void getAllUsers_mapsAdminFlag() {
        List<AdminUserData> users = adminService.getAllUsers();

        AdminUserData adminDto = findByEmail(users, "admin@sporixx.sk");
        assertTrue(adminDto.isAdmin(), "admin musí mať admin=true");
        assertFalse(adminDto.isFamilyManager(), "admin NIE je family manager");

        AdminUserData userDto = findByEmail(users, "marek.mosko@stuba.sk");
        assertFalse(userDto.isAdmin(), "bežný user nie je admin");
        assertFalse(userDto.isFamilyManager(), "bežný user nie je family manager");
    }

    @Test
    @DisplayName("DTO mapping – family manager flag")
    void getAllUsers_mapsFamilyManagerFlag() {
        List<AdminUserData> users = adminService.getAllUsers();

        AdminUserData fm = findByEmail(users, "jana.mrkvickova@stuba.sk");
        assertTrue(fm.isFamilyManager(), "family manager musí mať familyManager=true");
        assertFalse(fm.isAdmin(), "family manager nie je admin");
    }

    @Test
    @DisplayName("DTO mapping – full name = firstName + ' ' + lastName (trimnuté)")
    void getAllUsers_concatenatesFullName() {
        List<AdminUserData> users = adminService.getAllUsers();

        AdminUserData userDto = findByEmail(users, "marek.mosko@stuba.sk");
        assertEquals("Marek Moško", userDto.getName());
    }

    @Test
    @DisplayName("DTO mapping – active stav sa prenáša")
    void getAllUsers_preservesActiveFlag() {
        // vytvoríme deaktivovaného usera
        saveUser("inaktiv@test.sk", "In", "Aktiv", Role.USER, GenderCode.UNKNOWN, false);

        List<AdminUserData> users = adminService.getAllUsers();
        AdminUserData inactive = findByEmail(users, "inaktiv@test.sk");

        assertFalse(inactive.isActive(), "deaktivovaný user má active=false");
    }

    // ====================== EDGE CASES ======================

    @Test
    @DisplayName("Edge: ak sú iba admin a žiadni iní useri, zoznam má iba admina samotného")
    void getAllUsers_onlyAdminLeft_returnsSingleton() {
        // Nemôžeme reálne otestovať prázdnu DB, lebo potrebujeme admina v session
        // aby sme prešli requireAdmin(). Tu overíme edge kde admin vidí iba seba.
        userRepo.deleteById(regularUser.getId());
        userRepo.deleteById(familyManager.getId());

        List<AdminUserData> users = adminService.getAllUsers();
        assertEquals(1, users.size(), "zostal iba admin sám");
        assertNotNull(users, "vrátený list nesmie byť null");
        assertEquals(admin.getId(), users.get(0).getId());
    }

    @Test
    @DisplayName("Edge: user s null menom/priezviskom – DTO má empty string, nie NPE")
    void getAllUsers_nullFirstName_replacedWithEmptyString() {
        // directne vytvoríme usera s null-mi (ako keby bol zle inserted do DB)
        User broken = User.builder()
                .email("broken@test.sk")
                .firstName(null)
                .lastName(null)
                .passwordHash("xx")
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepo.save(broken);

        List<AdminUserData> users = adminService.getAllUsers();
        AdminUserData brokenDto = findByEmail(users, "broken@test.sk");

        // safe() metóda vráti "" pre null
        assertEquals("", brokenDto.getFirstName());
        assertEquals("", brokenDto.getLastName());
        assertEquals("", brokenDto.getName(), "name je trimnutý '' + ' ' + '' = ''");
    }

    @Test
    @DisplayName("Edge: user s null gender – DTO dostane GenderCode.UNKNOWN")
    void getAllUsers_nullGender_getsUnknownCode() {
        User noGender = User.builder()
                .email("nogender@test.sk")
                .firstName("No")
                .lastName("Gender")
                .passwordHash("xx")
                .role(Role.USER)
                .gender(null)
                .isActive(true)
                .build();
        userRepo.save(noGender);

        List<AdminUserData> users = adminService.getAllUsers();
        AdminUserData dto = findByEmail(users, "nogender@test.sk");

        assertEquals(GenderCode.UNKNOWN, dto.getGender(),
                "pri null gender sa použije UNKNOWN konštanta");
    }

    @Test
    @DisplayName("Edge: firstName s okrajovým whitespace sa trimne")
    void getAllUsers_trimsNameWhitespace() {
        User spaced = User.builder()
                .email("spaced@test.sk")
                .firstName("  Ján  ")
                .lastName("  Novák  ")
                .passwordHash("xx")
                .role(Role.USER)
                .isActive(true)
                .build();
        userRepo.save(spaced);

        List<AdminUserData> users = adminService.getAllUsers();
        AdminUserData dto = findByEmail(users, "spaced@test.sk");

        assertEquals("Ján", dto.getFirstName(), "firstName musí byť trimnutý");
        assertEquals("Novák", dto.getLastName(), "lastName musí byť trimnutý");
    }

    // ====================== AUTORIZÁCIA ======================

    @Test
    @DisplayName("Autorizácia: nie-admin volanie hodí ProfileException (invalid_credentials)")
    void getAllUsers_asRegularUser_throws() {
        loginAs(regularUser);

        ProfileException ex = assertThrows(ProfileException.class, () -> adminService.getAllUsers());
        assertEquals("auth.error.invalid_credentials", ex.getMessageKey());
    }

    @Test
    @DisplayName("Autorizácia: family manager nemá prístup")
    void getAllUsers_asFamilyManager_throws() {
        loginAs(familyManager);

        assertThrows(ProfileException.class, () -> adminService.getAllUsers());
    }

    @Test
    @DisplayName("Autorizácia: neprihlásený volajúci hodí ProfileException")
    void getAllUsers_notLoggedIn_throws() {
        logout();

        assertThrows(ProfileException.class, () -> adminService.getAllUsers());
    }

    // ====================== FAILING REPOSITORY ======================

    @Test
    @DisplayName("Ak userRepository.findAll() zlyhá, exception sa propaguje")
    void getAllUsers_repoFails_propagatesException() {
        // Vymeníme userRepo za failing variant a adminService treba re-inštanciovat
        UserRepository failing = new InMemoryUserRepository() {
            @Override
            public List<User> findAll() {
                throw new RuntimeException("Simulated DB failure");
            }
        };
        // znova prihlasime admina, pretože ideme použiť nový service s novým repo
        // ale repo je prazdne - takze session admin je stale validne nastavený
        AdminService svc = new AdminServiceImpl(failing, accountRepo, new InMemoryAccountAccessRepository(), userService);

        // Pozor: requireAdmin() sa vykoná SKÔR ako findAll() - preto lockneme sessiona
        // platne, akurat bezpecnostna kontrola preskoci ak SessionManager.isAdmin() == true
        RuntimeException ex = assertThrows(RuntimeException.class, svc::getAllUsers);
        assertEquals("Simulated DB failure", ex.getMessage());
    }

    // ====================== HELPER ======================

    private AdminUserData findByEmail(List<AdminUserData> list, String email) {
        return list.stream()
                .filter(d -> email.equalsIgnoreCase(d.getEmail()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User not found: " + email));
    }
}
