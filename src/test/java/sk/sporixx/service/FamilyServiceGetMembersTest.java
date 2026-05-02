package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.FamilyMemberData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre FamilyService.getFamilyMembers() – načítanie členov rodiny.
 */
@DisplayName("FamilyService – getFamilyMembers")
class FamilyServiceGetMembersTest extends FamilyServiceTestSupport {

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("manager s jedným dieťaťom dostane jeden záznam")
        void getFamilyMembers_oneMember_returnsOne() {
            List<FamilyMemberData> members = familyService.getFamilyMembers();
            assertEquals(1, members.size());
            assertEquals(child.getId(), members.get(0).getUserId());
        }

        @Test
        @DisplayName("vrátený člen obsahuje správne meno a email")
        void getFamilyMembers_memberHasCorrectData() {
            FamilyMemberData member = familyService.getFamilyMembers().get(0);
            assertEquals(child.getFirstName(), member.getFirstName());
            assertEquals(child.getLastName(),  member.getLastName());
            assertEquals(child.getEmail(),     member.getEmail());
        }

        @Test
        @DisplayName("vrátený člen obsahuje zoznam jeho účtov")
        void getFamilyMembers_memberHasAccounts() {
            FamilyMemberData member = familyService.getFamilyMembers().get(0);
            assertFalse(member.getAccounts().isEmpty());
            assertEquals(childMainAccount.getId(), member.getAccounts().get(0).getId());
        }

        @Test
        @DisplayName("manager bez detí dostane prázdny zoznam")
        void getFamilyMembers_noChildren_returnsEmpty() {
            // Prihlásime stranger-a, ktorý nemá žiadne prístupy k cudzím účtom
            loginAs(stranger);
            List<FamilyMemberData> members = familyService.getFamilyMembers();
            assertTrue(members.isEmpty());
        }

        @Test
        @DisplayName("vlastné účty managera sa do zoznamu NEpočítajú")
        void getFamilyMembers_ownAccounts_notIncluded() {
            // dáme managerovi vlastný účet a prístup k nemu
            var managerAccount = giveMainAccount(manager.getId());
            accountAccessRepo.grantAccess(manager.getId(), managerAccount.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());

            List<FamilyMemberData> members = familyService.getFamilyMembers();
            // stále len child, nie manager sám seba
            assertEquals(1, members.size());
            assertEquals(child.getId(), members.get(0).getUserId());
        }

        @Test
        @DisplayName("manager s dvoma deťmi dostane dva záznamy")
        void getFamilyMembers_twoChildren_returnsTwo() {
            accountAccessRepo.grantAccess(manager.getId(), child2MainAccount.getId(),
                    sk.sporixx.model.Role.USER.getAccessLevel());

            List<FamilyMemberData> members = familyService.getFamilyMembers();
            assertEquals(2, members.size());
        }
    }
}

