package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.RecurringRule;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link RecurringRuleService}.
 *
 * Pokrývame:
 *   - načítanie pravidiel
 *   - validácia pri pridaní pravidla (suma, popis, klasifikácia, dátumy, interval)
 *   - úspešné pridanie pravidla s budúcim startDate
 *   - aktualizácia pravidla (validácia)
 *   - deaktivácia (soft delete)
 */
@DisplayName("RecurringRuleService – Management")
class ManagementRecurringRuleServiceTest extends ManagementServiceTestSupport {

    private static final LocalDate FUTURE = LocalDate.now().plusMonths(1);
    private static final LocalDate PAST   = LocalDate.now().minusDays(1);

    // ======================== GET RECURRING RULES ========================

    @Nested
    @DisplayName("Načítanie pravidiel")
    class GetRules {

        @Test
        @DisplayName("Žiadne pravidlá → prázdny zoznam")
        void noRules_returnsEmpty() {
            assertTrue(recurringRuleService.getRecurringRules().isEmpty());
        }

        @Test
        @DisplayName("Pridané pravidlá sú vrátené")
        void addedRules_returned() {
            recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);

            List<RecurringRule> rules = recurringRuleService.getRecurringRules();
            assertEquals(1, rules.size());
            assertEquals("Nájom", rules.get(0).getDescription());
        }

        @Test
        @DisplayName("Pravidlá zo všetkých účtov sú vrátené")
        void rulesFromMultipleAccounts_returned() {
            recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);
            recurringRuleService.addRecurringRule(
                    emergencyAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Poistenie", 100.0, "MONTHLY", 1, FUTURE, null);

            assertEquals(2, recurringRuleService.getRecurringRules().size());
        }

        @Test
        @DisplayName("Deaktivované pravidlo sa nezobrazí")
        void deactivatedRule_notReturned() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);

            recurringRuleService.deleteRecurringRule(rule.getId());

            assertTrue(recurringRuleService.getRecurringRules().isEmpty());
        }
    }

    // ======================== ADD RECURRING RULE – VALIDÁCIA ========================

    @Nested
    @DisplayName("Pridanie pravidla – validácia")
    class AddRuleValidation {

        @Test
        @DisplayName("Platné údaje → pravidlo sa vytvorí")
        void validInput_ruleCreated() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);

            assertNotNull(rule);
            assertEquals("Nájom", rule.getDescription());
            assertEquals(500.0, rule.getAmount(), 0.001);
            assertTrue(rule.isActive());
        }

        @Test
        @DisplayName("Suma ≤ 0 → RecurringRuleException")
        void zeroAmount_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 0.0, "MONTHLY", 1, FUTURE, null));
        }

        @Test
        @DisplayName("Záporná suma → RecurringRuleException")
        void negativeAmount_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", -50.0, "MONTHLY", 1, FUTURE, null));
        }

        @Test
        @DisplayName("Prázdny popis → RecurringRuleException")
        void blankDescription_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "  ", 500.0, "MONTHLY", 1, FUTURE, null));
        }

        @Test
        @DisplayName("Null popis → RecurringRuleException")
        void nullDescription_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            null, 500.0, "MONTHLY", 1, FUTURE, null));
        }

        @Test
        @DisplayName("Null klasifikácia → RecurringRuleException")
        void nullClassification_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, null,
                            "Nájom", 500.0, "MONTHLY", 1, FUTURE, null));
        }

        @Test
        @DisplayName("Null startDate → RecurringRuleException")
        void nullStartDate_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 500.0, "MONTHLY", 1, null, null));
        }

        @Test
        @DisplayName("Interval ≤ 0 → RecurringRuleException")
        void zeroInterval_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 500.0, "MONTHLY", 0, FUTURE, null));
        }

        @Test
        @DisplayName("Neznámy accountId → RecurringRuleException")
        void unknownAccount_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            999, 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 500.0, "MONTHLY", 1, FUTURE, null));
        }

        @Test
        @DisplayName("endDate pred startDate → RecurringRuleException")
        void endBeforeStart_throwsException() {
            LocalDate start = LocalDate.now().plusDays(10);
            LocalDate end   = LocalDate.now().plusDays(5);
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 500.0, "MONTHLY", 1, start, end));
        }

        @Test
        @DisplayName("endDate v minulosti → RecurringRuleException")
        void endInPast_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.addRecurringRule(
                            mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 500.0, "MONTHLY", 1, FUTURE, PAST));
        }
    }

    // ======================== ADD RECURRING RULE – FREQUENCY TYPES ========================

    @Nested
    @DisplayName("Pridanie pravidla – typy frekvencie")
    class FrequencyTypes {

        @Test
        @DisplayName("Frekvencia DAILY sa uloží správne")
        void dailyFrequency_saved() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Denná platba", 5.0, "DAILY", 1, FUTURE, null);
            assertEquals("DAILY", rule.getFrequencyType());
        }

        @Test
        @DisplayName("Frekvencia WEEKLY sa uloží správne")
        void weeklyFrequency_saved() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Týždenná platba", 20.0, "WEEKLY", 2, FUTURE, null);
            assertEquals("WEEKLY", rule.getFrequencyType());
            assertEquals(2, rule.getFrequencyInterval());
        }

        @Test
        @DisplayName("Frekvencia YEARLY sa uloží správne")
        void yearlyFrequency_saved() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_WANT,
                    "Ročné predplatné", 99.0, "YEARLY", 1, FUTURE, null);
            assertEquals("YEARLY", rule.getFrequencyType());
        }

        @Test
        @DisplayName("Interval > 1 sa uloží správne")
        void intervalGreaterThanOne_saved() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Každé 3 mesiace", 150.0, "MONTHLY", 3, FUTURE, null);
            assertEquals(3, rule.getFrequencyInterval());
        }

        @Test
        @DisplayName("Pravidlo s endDate sa uloží správne")
        void withEndDate_saved() {
            LocalDate end = FUTURE.plusMonths(6);
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Obmedzená platba", 100.0, "MONTHLY", 1, FUTURE, end);
            assertNotNull(rule.getEndDate());
        }

        @Test
        @DisplayName("CLASSIFICATION_WANT je platná klasifikácia")
        void wantClassification_accepted() {
            assertDoesNotThrow(() -> recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_WANT,
                    "Zábava", 30.0, "MONTHLY", 1, FUTURE, null));
        }
    }

    // ======================== UPDATE – EDGE CASES ========================

    @Nested
    @DisplayName("Aktualizácia pravidla – edge cases")
    class UpdateRuleEdgeCases {

        @Test
        @DisplayName("Aktualizácia s novou endDate v budúcnosti je povolená")
        void updateWithFutureEndDate_allowed() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);
            LocalDate newEnd = LocalDate.now().plusMonths(3);
            assertDoesNotThrow(() -> recurringRuleService.updateRecurringRule(
                    rule.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, newEnd));
        }

        @Test
        @DisplayName("Prázdny popis pri update → RecurringRuleException")
        void blankDescriptionUpdate_throwsException() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.updateRecurringRule(
                            rule.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "", 500.0, "MONTHLY", 1, null));
        }

        @Test
        @DisplayName("Interval ≤ 0 pri update → RecurringRuleException")
        void zeroIntervalUpdate_throwsException() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.updateRecurringRule(
                            rule.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", 500.0, "MONTHLY", 0, null));
        }
    }

    @Nested
    @DisplayName("Aktualizácia pravidla")
    class UpdateRule {

        @Test
        @DisplayName("Platná aktualizácia → pravidlo sa zmení")
        void validUpdate_ruleUpdated() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);

            recurringRuleService.updateRecurringRule(
                    rule.getId(), 1, Transaction.CLASSIFICATION_WANT,
                    "Nájom updated", 600.0, "MONTHLY", 1, null);

            RecurringRule updated = recurringRuleService.getRecurringRules().get(0);
            assertEquals("Nájom updated", updated.getDescription());
            assertEquals(600.0, updated.getAmount(), 0.001);
        }

        @Test
        @DisplayName("Neznáme ID → RecurringRuleException")
        void unknownId_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.updateRecurringRule(
                            999, 1, Transaction.CLASSIFICATION_NEED,
                            "X", 100.0, "MONTHLY", 1, null));
        }

        @Test
        @DisplayName("Záporná suma pri update → RecurringRuleException")
        void negativeAmount_throwsException() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);

            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.updateRecurringRule(
                            rule.getId(), 1, Transaction.CLASSIFICATION_NEED,
                            "Nájom", -10.0, "MONTHLY", 1, null));
        }
    }

    // ======================== DELETE RECURRING RULE ========================

    @Nested
    @DisplayName("Deaktivácia pravidla")
    class DeleteRule {

        @Test
        @DisplayName("Existujúce pravidlo sa deaktivuje")
        void existingRule_deactivated() {
            RecurringRule rule = recurringRuleService.addRecurringRule(
                    mainAccount.getId(), 1, Transaction.CLASSIFICATION_NEED,
                    "Nájom", 500.0, "MONTHLY", 1, FUTURE, null);

            recurringRuleService.deleteRecurringRule(rule.getId());

            assertTrue(recurringRuleService.getRecurringRules().isEmpty());
        }

        @Test
        @DisplayName("Neznáme ID → RecurringRuleException")
        void unknownId_throwsException() {
            assertThrows(RecurringRuleException.class,
                    () -> recurringRuleService.deleteRecurringRule(999));
        }
    }
}

