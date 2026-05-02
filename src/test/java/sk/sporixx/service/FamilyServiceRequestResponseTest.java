package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.FamilyRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre FamilyService.acceptFamilyRequest() a rejectFamilyRequest().
 */
@DisplayName("FamilyService – accept/rejectFamilyRequest")
class FamilyServiceRequestResponseTest extends FamilyServiceTestSupport {

    // ─── acceptFamilyRequest ──────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptFamilyRequest – happy path")
    class AcceptHappyPath {

        @Test
        @DisplayName("po prijatí sa status zmení na ACCEPTED")
        void accept_valid_statusChangedToAccepted() {
            FamilyRequest req = sendRequest(manager.getId(), child2.getId());
            loginAs(child2);

            familyService.acceptFamilyRequest(req.getId());

            assertEquals(FamilyRequest.STATUS_ACCEPTED,
                    familyRequestRepo.findById(req.getId()).get().getStatus());
        }

        @Test
        @DisplayName("po prijatí manager dostane prístup k účtom dieťaťa")
        void accept_valid_managerGetsAccess() {
            FamilyRequest req = sendRequest(manager.getId(), child2.getId());
            loginAs(child2);

            familyService.acceptFamilyRequest(req.getId());

            boolean hasAccess = accountAccessRepo.findByUserId(manager.getId())
                    .stream()
                    .anyMatch(a -> a.getAccountId() == child2MainAccount.getId());
            assertTrue(hasAccess);
        }
    }

    @Nested
    @DisplayName("acceptFamilyRequest – chybové scenáre")
    class AcceptErrors {

        @Test
        @DisplayName("neexistujúca požiadavka hodí FamilyException")
        void accept_notFound_throws() {
            loginAs(child2);
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.acceptFamilyRequest(999));
            assertEquals("family.error.request_not_found", ex.getMessageKey());
        }

        @Test
        @DisplayName("iný user sa pokúsi akceptovať cudziu požiadavku → chyba")
        void accept_wrongUser_throws() {
            FamilyRequest req = sendRequest(manager.getId(), child2.getId());
            loginAs(child); // child nemá byť príjemcom

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.acceptFamilyRequest(req.getId()));
            assertEquals("family.error.not_your_request", ex.getMessageKey());
        }

        @Test
        @DisplayName("FAMILY_MANAGER nemôže byť zároveň dieťaťom")
        void accept_childIsManager_throws() {
            // manager dostane requesty od niekoho iného
            var otherManager = saveUser("mgr2@test.sk", "Iný", "Manažér", sk.sporixx.model.Role.FAMILY_MANAGER);
            FamilyRequest req = sendRequest(otherManager.getId(), manager.getId());
            loginAs(manager);

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.acceptFamilyRequest(req.getId()));
            assertEquals("family.error.already_parent", ex.getMessageKey());
        }

        @Test
        @DisplayName("max 2 rodičia – tretí pokus hodí FamilyException")
        void accept_maxParentsReached_throws() {
            // child2 má už dvoch rodičov (simulácia: dvaja manažéri majú prístup k jeho účtu)
            var mgr2 = saveUser("mgr2@test.sk", "Druhý", "Manager", sk.sporixx.model.Role.FAMILY_MANAGER);
            var mgr3 = saveUser("mgr3@test.sk", "Tretí", "Manager", sk.sporixx.model.Role.FAMILY_MANAGER);
            accountAccessRepo.grantAccess(mgr2.getId(), child2MainAccount.getId(), 1);
            accountAccessRepo.grantAccess(mgr3.getId(), child2MainAccount.getId(), 1);

            // teraz pridáme 4. požiadavku
            var mgr4 = saveUser("mgr4@test.sk", "Štvrtý", "Manager", sk.sporixx.model.Role.FAMILY_MANAGER);
            FamilyRequest req = sendRequest(mgr4.getId(), child2.getId());
            loginAs(child2);

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.acceptFamilyRequest(req.getId()));
            assertEquals("family.error.max_parents_reached", ex.getMessageKey());
        }
    }

    // ─── rejectFamilyRequest ─────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectFamilyRequest")
    class Reject {

        @Test
        @DisplayName("po odmietnutí sa status zmení na REJECTED")
        void reject_valid_statusChangedToRejected() {
            FamilyRequest req = sendRequest(manager.getId(), child2.getId());
            loginAs(child2);

            familyService.rejectFamilyRequest(req.getId());

            assertEquals(FamilyRequest.STATUS_REJECTED,
                    familyRequestRepo.findById(req.getId()).get().getStatus());
        }

        @Test
        @DisplayName("odmietnutie cudzej požiadavky hodí FamilyException")
        void reject_wrongUser_throws() {
            FamilyRequest req = sendRequest(manager.getId(), child2.getId());
            loginAs(child);

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.rejectFamilyRequest(req.getId()));
            assertEquals("family.error.not_your_request", ex.getMessageKey());
        }

        @Test
        @DisplayName("neexistujúca požiadavka hodí FamilyException")
        void reject_notFound_throws() {
            loginAs(child2);
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.rejectFamilyRequest(999));
            assertEquals("family.error.request_not_found", ex.getMessageKey());
        }
    }
}

