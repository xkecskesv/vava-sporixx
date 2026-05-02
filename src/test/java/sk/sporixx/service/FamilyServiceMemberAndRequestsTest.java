package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre FamilyService.removeFamilyMember() a getPendingRequests() a getSentRequests().
 */
@DisplayName("FamilyService – remove, pendingRequests, sentRequests")
class FamilyServiceMemberAndRequestsTest extends FamilyServiceTestSupport {

    // ─── removeFamilyMember ──────────────────────────────────────────────────

    @Nested
    @DisplayName("removeFamilyMember")
    class Remove {

        @Test
        @DisplayName("odstránenie člena zruší všetky prístupy managera k jeho účtom")
        void remove_valid_accessRevoked() {
            familyService.removeFamilyMember(child.getId());

            boolean stillHasAccess = accountAccessRepo.findByUserId(manager.getId())
                    .stream()
                    .anyMatch(a -> a.getAccountId() == childMainAccount.getId());
            assertFalse(stillHasAccess);
        }

        @Test
        @DisplayName("po odstránení getFamilyMembers vráti prázdny zoznam")
        void remove_valid_membersEmpty() {
            familyService.removeFamilyMember(child.getId());
            assertTrue(familyService.getFamilyMembers().isEmpty());
        }

        @Test
        @DisplayName("odstránenie usera ktorý nie je členom rodiny hodí FamilyException")
        void remove_notMember_throws() {
            FamilyException ex = assertThrows(FamilyException.class,
                    () -> familyService.removeFamilyMember(stranger.getId()));
            assertEquals("family.error.not_a_member", ex.getMessageKey());
        }
    }

    // ─── getPendingRequests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getPendingRequests")
    class PendingRequests {

        @Test
        @DisplayName("vráti pending požiadavky pre prihlásený user")
        void getPendingRequests_hasPending_returnsOne() {
            sendRequest(manager.getId(), child2.getId());
            loginAs(child2);

            var pending = familyService.getPendingRequests();
            assertEquals(1, pending.size());
            assertEquals(manager.getId(), pending.get(0).getFromUserId());
        }

        @Test
        @DisplayName("po akceptácii sa požiadavka nevracia ako pending")
        void getPendingRequests_afterAccept_empty() {
            var req = sendRequest(manager.getId(), child2.getId());
            loginAs(child2);
            familyService.acceptFamilyRequest(req.getId());

            assertTrue(familyService.getPendingRequests().isEmpty());
        }

        @Test
        @DisplayName("žiadne pending požiadavky → prázdny zoznam")
        void getPendingRequests_noPending_empty() {
            loginAs(child2);
            assertTrue(familyService.getPendingRequests().isEmpty());
        }

        @Test
        @DisplayName("vrátený záznam obsahuje správne meno odosielateľa")
        void getPendingRequests_containsSenderName() {
            sendRequest(manager.getId(), child2.getId());
            loginAs(child2);

            var pending = familyService.getPendingRequests();
            assertEquals(manager.getFirstName(), pending.get(0).getFromFirstName());
            assertEquals(manager.getLastName(),  pending.get(0).getFromLastName());
        }
    }

    // ─── getSentRequests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSentRequests")
    class SentRequests {

        @Test
        @DisplayName("vráti odoslané pending požiadavky manažéra")
        void getSentRequests_hasSent_returnsOne() {
            sendRequest(manager.getId(), child2.getId());

            var sent = familyService.getSentRequests();
            assertEquals(1, sent.size());
            assertEquals(child2.getId(), sent.get(0).getToUserId());
        }

        @Test
        @DisplayName("žiadne odoslané požiadavky → prázdny zoznam")
        void getSentRequests_noSent_empty() {
            assertTrue(familyService.getSentRequests().isEmpty());
        }

        @Test
        @DisplayName("po akceptácii sa požiadavka nevracia ako pending sent")
        void getSentRequests_afterAccept_empty() {
            var req = sendRequest(manager.getId(), child2.getId());
            loginAs(child2);
            familyService.acceptFamilyRequest(req.getId());
            loginAs(manager);

            assertTrue(familyService.getSentRequests().isEmpty());
        }
    }
}

