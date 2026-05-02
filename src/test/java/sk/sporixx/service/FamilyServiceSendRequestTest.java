package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.FamilyRequest;
import sk.sporixx.model.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre FamilyService.sendFamilyRequest() – odoslanie rodinnej požiadavky.
 */
@DisplayName("FamilyService – sendFamilyRequest")
class FamilyServiceSendRequestTest extends FamilyServiceTestSupport {

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("platná požiadavka sa uloží ako PENDING")
        void sendRequest_valid_savedAsPending() {
            familyService.sendFamilyRequest(child2.getEmail());

            List<FamilyRequest> sent = familyRequestRepo.findAll();
            assertEquals(1, sent.size());
            assertEquals(FamilyRequest.STATUS_PENDING, sent.get(0).getStatus());
            assertEquals(manager.getId(), sent.get(0).getFromUserId());
            assertEquals(child2.getId(),  sent.get(0).getToUserId());
        }

        @Test
        @DisplayName("email sa porovnáva case-insensitive")
        void sendRequest_emailCaseInsensitive_works() {
            assertDoesNotThrow(() ->
                    familyService.sendFamilyRequest(child2.getEmail().toUpperCase()));
        }
    }

    @Nested
    @DisplayName("Validácia vstupov")
    class Validation {

        @Test
        @DisplayName("prázdny email hodí FamilyException")
        void sendRequest_blankEmail_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(""));
            assertEquals("family.error.email_required", ex.getMessageKey());
        }

        @Test
        @DisplayName("null email hodí FamilyException")
        void sendRequest_nullEmail_throws() {
            assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(null));
        }

        @Test
        @DisplayName("neexistujúci email hodí FamilyException")
        void sendRequest_unknownEmail_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest("nikto@test.sk"));
            assertEquals("family.error.user_not_found", ex.getMessageKey());
        }

        @Test
        @DisplayName("target nie je USER (je FAMILY_MANAGER) → chyba")
        void sendRequest_targetIsManager_throws() {
            // urobíme druhého managera
            var otherManager = saveUser("mgr2@test.sk", "Iný", "Manažér", Role.FAMILY_MANAGER);
            giveMainAccount(otherManager.getId());

            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(otherManager.getEmail()));
            assertEquals("family.error.not_a_user", ex.getMessageKey());
        }

        @Test
        @DisplayName("manager nemôže pridať sám seba (jeho rola nie je USER → not_a_user)")
        void sendRequest_self_throws() {
            // manager má rolu FAMILY_MANAGER – kód najskôr overí rolu (nie je USER),
            // takže dostaneme not_a_user skôr ako cannot_add_self
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(manager.getEmail()));
            assertEquals("family.error.not_a_user", ex.getMessageKey());
        }

        @Test
        @DisplayName("child bez účtov → chyba")
        void sendRequest_childHasNoAccounts_throws() {
            var noAccChild = saveUser("noaccounts@test.sk", "Bez", "Účtov", Role.USER);
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(noAccChild.getEmail()));
            assertEquals("family.error.no_accounts", ex.getMessageKey());
        }

        @Test
        @DisplayName("child je už členom rodiny → chyba")
        void sendRequest_alreadyMember_throws() {
            // child je už v rodine (nastavené v baseSetUp)
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(child.getEmail()));
            assertEquals("family.error.already_member", ex.getMessageKey());
        }

        @Test
        @DisplayName("duplicitná pending požiadavka → chyba")
        void sendRequest_duplicate_throws() {
            familyService.sendFamilyRequest(child2.getEmail());
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.sendFamilyRequest(child2.getEmail()));
            assertEquals("family.error.request_already_sent", ex.getMessageKey());
        }
    }
}


