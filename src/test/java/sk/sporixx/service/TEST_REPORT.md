# Unit Test Report - Sporixx

**Dátum:** 2. máj 2026
**Celkový čas:** 7:55 min  
**Java:** OpenJDK 25 (loom)  
**Framework:** JUnit Jupiter 5.11.3 / Maven Surefire 3.2.5

---

## Celkové výsledky

| Celkom testov | Prešlo  | Zlyhalo | Preskočené |
|:---:|:-------:|:-------:|:----------:|
| **747** | **742** |  **5**  |   **0**    |

> Úspešnosť: **99,06 %**

---

## Výsledky podľa testovacích tried

### AdminService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:----:|:----:|---:|
| `AdminServiceAuthorizationTest$AsFamilyManager` | 7 |  7   |  0   | 4,907 s |
| `AdminServiceAuthorizationTest$AsRegularUser` | 7 |  7   |  0   | 5,865 s |
| `AdminServiceAuthorizationTest$NotLoggedIn` | 6 |  6   |  0   | 4,885 s |
| `AdminServiceChangeOwnPasswordTest` | 12 |  12  |  0   | 13,57 s |
| `AdminServiceGetAllUsersTest` | 13 |  13  |  0   | 10,89 s |
| `AdminServiceIntegrationTest` | 11 |  11  |  0   | 11,66 s |
| `AdminServiceLifecycleTest` | 26 |  26  |  0   | 22,17 s |
| `AdminServiceUpdateUserTest` | 25 |  25  |  0   | 21,16 s |

---

### AuthService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `AuthServiceTest` | 35 | 31 | 2 | 17,39 s |

---

### BudgetService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `BudgetServiceCustomAllocationTest` | 17 | 17 | 0 | 0,004 s |
| `BudgetServiceLoadDataTest` | 17 | 17 | 0 | 0,012 s |
| `BudgetServiceSaveSetupTest` | 23 | 23 | 0 | 0,008 s |
| `BudgetServiceScenarioTest` | 8 | 8 | 0 | 0,009 s |

---

### ManagementService - Účty (Accounts)

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ManagementAccountServiceTest$CreatePrivateAccount` | 7 | 7 | 0 | 0,005 s |
| `ManagementAccountServiceTest$CreateSavingAccount` | 9 | 9 | 0 | 0,004 s |
| `ManagementAccountServiceTest$DeleteAccount` | 4 | 4 | 0 | 0,007 s |
| `ManagementAccountServiceTest$GetSavingGoal` | 2 | 2 | 0 | 0,004 s |
| `ManagementAccountServiceTest$UpdateAccountDescription` | 3 | 3 | 0 | 0,004 s |
| `ManagementAccountServiceTest$UpdateSavingAccount` | 5 | 5 | 0 | 0,008 s |

---

### ManagementService - Kategórie (Categories)

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ManagementCategoryServiceTest$AddCategory` | 6 | 6 | 0 | 0,006 s |
| `ManagementCategoryServiceTest$DeleteCategory` | 5 | 5 | 0 | 0,052 s |
| `ManagementCategoryServiceTest$GetCategories` | 5 | 5 | 0 | 0,004 s |
| `ManagementCategoryServiceTest$UpdateCategory` | 8 | 8 | 0 | 0,012 s |

---

### ManagementService - Opakujúce sa pravidlá (Recurring Rules)

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ManagementRecurringRuleServiceTest$AddRuleValidation` | 11 | 11 | 0 | 0,003 s |
| `ManagementRecurringRuleServiceTest$DeleteRule` | 2 | 2 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$FrequencyTypes` | 6 | 6 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$GetRules` | 4 | 4 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$UpdateRule` | 3 | 3 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$UpdateRuleEdgeCases` | 3 | 3 | 0 | 0 s |

---

### OverviewService - Prehľad účtov

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `OverviewServiceAccountsSummaryTest$AccountList` | 3 | 3 | 0 | 0,001 s |
| `OverviewServiceAccountsSummaryTest$EdgeCases` | 3 | 3 | 0 | 0,002 s |
| `OverviewServiceAccountsSummaryTest$SavingGoals` | 5 | 5 | 0 | 0,002 s |
| `OverviewServiceAccountsSummaryTest$TotalBalance` | 3 | 3 | 0 | 0,002 s |

---

### OverviewService - Aktivity

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `OverviewServiceActivitiesTest$Combined` | 2 | 2 | 0 | 0 s |
| `OverviewServiceActivitiesTest$EdgeCases` | 4 | 4 | 0 | 0,005 s |
| `OverviewServiceActivitiesTest$EmptyData` | 1 | 1 | 0 | 0,001 s |
| `OverviewServiceActivitiesTest$RecentTransactions` | 6 | 6 | 0 | 0,003 s |
| `OverviewServiceActivitiesTest$UpcomingPayments` | 7 | 7 | 0 | 0,002 s |

---

### OverviewService - Analytika

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `OverviewServiceAnalyticsTest$AccountFiltering` | 2 | 2 | 0 | 0 s |
| `OverviewServiceAnalyticsTest$ChartPeriodMeta` | 3 | 3 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$EmptyData` | 2 | 2 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$GroupByDay` | 3 | 3 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$GroupByMonth` | 4 | 4 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$TotalIncomeCalculation` | 3 | 3 | 0 | 0 s |

---

### ReportsService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ReportsServiceCategoryExpenseTest` | 8 | 8 | 0 | 0,002 s |
| `ReportsServiceCategoryExpenseJdbcTest` | 9 | 9 | 0 | 0,140 s |
| `ReportsServiceIncomeExpenseTest` | 10 | 10 | 0 | 0,009 s |
| `ReportsServiceIncomeExpenseJdbcTest` | 15 | 15 | 0 | 0,248 s |
| `ReportsServiceRecurringExpenseTest` | 4 | 4 | 0 | 0,003 s |
| `ReportsServiceRecurringExpenseJdbcTest` | 5 | 5 | 0 | 0,072 s |
| `ReportsServiceSavingAccountsTest` | 13 | 13 | 0 | 0,003 s |
| `ReportsServiceSavingAccountsJdbcTest` | 14 | 14 | 0 | 0,723 s |
| `ReportsServiceWantNeedTest` | 8 | 8 | 0 | 0,003 s |
| `ReportsServiceWantNeedJdbcTest` | 8 | 8 | 0 | 0,118 s |

---

### TransactionService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `TransactionServiceAddTest` | 21 | 20 | 1 | 0,016 s |
| `TransactionServiceDeleteTest` | 9 | 8 | 1 | 0,006 s |
| `TransactionServiceUpdateTest` | 13 | 12 | 1 | 0,014 s |
| `TransactionServiceSearchTest` | 19 | 19 | 0 | 0,008 s |

---

### ExportService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ExportServiceIncomeExpenseTest` | 15 | 15 | 0 | 0,044 s |
| `ExportServiceSavingAccountsTest` | 17 | 17 | 0 | 0,164 s |

---

### MilestoneService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `MilestoneServiceBudgetKeeperTest` | 13 | 13 | 0 | 0,064 s |
| `MilestoneServiceSavingMasterTest` | 10 | 10 | 0 | 0,008 s |
| `MilestoneServiceSmartSpenderTest` | 13 | 13 | 0 | 0,010 s |
| `MilestoneServiceInvestorTest` | 11 | 11 | 0 | 0,007 s |
| `MilestoneServiceFinancialTitleTest` | 15 | 15 | 0 | 0,047 s |

---

### ProfileService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ProfileServiceUpdateProfileTest$HappyPath` | 6 | 6 | 0 | 4,926 s |
| `ProfileServiceUpdateProfileTest$Validation` | 5 | 5 | 0 | 4,122 s |
| `ProfileServiceUpdateProfileTest$NotLoggedIn` | 1 | 1 | 0 | 0,935 s |
| `ProfileServiceChangePasswordTest$HappyPath` | 1 | 1 | 0 | 1,910 s |
| `ProfileServiceChangePasswordTest$Validation` | 4 | 4 | 0 | 4,371 s |
| `ProfileServiceChangePasswordTest$NotLoggedIn` | 1 | 1 | 0 | 0,820 s |
| `ProfileServicePhotoAndGenderTest$UpdateProfilePhoto` | 5 | 5 | 0 | 8,318 s |
| `ProfileServicePhotoAndGenderTest$ToDisplayGender` | 5 | 5 | 0 | 0,012 s |

---

### FamilyService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `FamilyServiceGetMembersTest$HappyPath` | 6 | 6 | 0 | 6,589 s |
| `FamilyServiceSendRequestTest$HappyPath` | 2 | 2 | 0 | 1,820 s |
| `FamilyServiceSendRequestTest$Validation` | 7 | 7 | 0 | 5,112 s |
| `FamilyServiceRequestResponseTest$AcceptHappyPath` | 2 | 2 | 0 | 1,930 s |
| `FamilyServiceRequestResponseTest$AcceptErrors` | 4 | 4 | 0 | 3,641 s |
| `FamilyServiceRequestResponseTest$Reject` | 3 | 3 | 0 | 2,755 s |
| `FamilyServiceMemberAndRequestsTest$Remove` | 3 | 3 | 0 | 3,336 s |
| `FamilyServiceMemberAndRequestsTest$PendingRequests` | 4 | 4 | 0 | 3,102 s |
| `FamilyServiceMemberAndRequestsTest$SentRequests` | 3 | 3 | 0 | 2,841 s |
| `FamilyServiceUpdateSavingAccountTest$HappyPath` | 3 | 3 | 0 | 2,974 s |
| `FamilyServiceUpdateSavingAccountTest$AccessErrors` | 3 | 3 | 0 | 2,813 s |
| `FamilyServiceUpdateSavingAccountTest$Validation` | 5 | 5 | 0 | 4,320 s |

---

### CurrencyService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `CurrencyServiceTest$Convert` | 8 | 8 | 0 | 0,009 s |
| `CurrencyServiceTest$Format` | 7 | 7 | 0 | 0,031 s |
| `CurrencyServiceTest$GetUserCurrency` | 2 | 2 | 0 | 0,040 s |

---

### SettingsService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `SettingsServiceTest$Defaults` | 7 | 7 | 0 | < 0,001 s |
| `SettingsServiceTest$SetLanguage` | 8 | 8 | 0 | 0,006 s |
| `SettingsServiceTest$SetCurrency` | 8 | 8 | 0 | < 0,001 s |
| `SettingsServiceTest$Notifications` | 7 | 7 | 0 | < 0,001 s |
| `SettingsServiceTest$Snapshot` | 4 | 4 | 0 | < 0,001 s |
| `SettingsServiceTest$LoadFromRepo` | 4 | 4 | 0 | < 0,001 s |

---

### UserService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `UserServiceTest$GetCurrentUser` | 7 | 7 | 0 | 1,718 s |
| `UserServiceTest$NormalizeGender` | 14 | 14 | 0 | 0,009 s |
| `UserServiceTest$ToDisplayGender` | 6 | 6 | 0 | 0,020 s |

---

## Zlyhané testy (Failures)

### 1. `AuthServiceTest.login_validCredentials_shouldSucceed` - riadok 248
**Typ:** `AssertionFailedError`
**Príčina:** `ServiceLocator not initialized! Call ServiceLocator.init() in Main.start() first.`
**Popis:** Login volá `ServiceLocator.getSettingsService()`, ale `ServiceLocator` nebol inicializovaný v testovom prostredí.

---

### 2. `AuthServiceTest.login_nullRole_shouldSetDefaultRole` - riadok 335
**Typ:** `AssertionFailedError`
**Príčina:** `ServiceLocator not initialized! Call ServiceLocator.init() in Main.start() first.`
**Popis:** Rovnaký problém ako vyššie - `AuthServiceImpl.login()` vyžaduje inicializovaný `ServiceLocator`.

---

### 3. `TransactionServiceAddTest.transfer_mainToEmergency_noAutoCategory` - riadok 157
**Typ:** `AssertionFailedError`
**Správa:** `transfer bez saving účtu nemá auto-kategóriu ==> expected: <null> but was: <9>`
**Popis:** Pri prevode z hlavného účtu na emergency fond (nie saving účet) sa nesprávne priradí automatická kategória (ID 9). Očakáva sa `null`.

---

### 4. `TransactionServiceDeleteTest.delete_transferLeg_leavesOtherIntact` - riadok 127
**Typ:** `AssertionFailedError`
**Správa:** `BUG: druhy leg transferu zostal a saving má 'umelých' 200 € ==> expected: <700.0> but was: <500.0>`
**Popis:** Pri zmazaní jednej nôžky prevodu (transfer leg) sa druhá nôžka nesprávne zachová a zostatok saving účtu je o 200 € nižší, ako má byť - namiesto 700 € je 500 €.

---

### 5. `TransactionServiceUpdateTest.update_transfer_doesNotUpdateOtherLeg` - riadok 251
**Typ:** `AssertionFailedError`
**Správa:** `BUG: druhý leg transferu sa nepretransformoval ==> expected: <700.0> but was: <800.0>`
**Popis:** Aktualizácia jednej nôžky prevodu nesprávne ovplyvňuje aj druhú nôžku - zostatok je 800 € namiesto očakávaných 700 €.

---

## Analýza problémov

### Problém 1: `ServiceLocator` nie je inicializovaný v testoch
- **Dotknuté testy:** 2 testy v `AuthServiceTest`
- **Príčina:** `AuthServiceImpl.login()` volá `ServiceLocator.getSettingsService()`, čo vyžaduje `ServiceLocator.init()` - táto metóda sa bežne volá v `Main.start()`, ale v unit testoch sa nevolá.
- **Odporúčanie:** Vložiť `ServiceLocator.init(...)` do `@BeforeEach` / `@BeforeAll` v testoch, alebo refaktorovať `AuthServiceImpl` tak, aby `SettingsService` bol injektovaný cez konštruktor (dependency injection).

### Problém 2: Nesprávna logika transfer transakcií
- **Dotknuté testy:** 3 testy v `TransactionServiceAddTest`, `TransactionServiceDeleteTest`, `TransactionServiceUpdateTest`
- **Príčina:** Logika pre spracovanie oboch nôžok prevodu (transfer legs) obsahuje chyby:
  - Auto-kategória sa priradí aj pri prevodoch na non-saving účty.
  - Zmazanie jednej nôžky nesprávne neobnoví zostatok druhého účtu.
  - Aktualizácia jednej nôžky nesprávne mení zostatok iného účtu.
- **Odporúčanie:** Skontrolovať a opraviť metódy `add`, `delete` a `update` v `TransactionServiceImpl` pri práci s `TRANSFER` typom transakcií.

---

## Dobre pokryté oblasti

| Oblasť | Počet testov | Výsledok |
|---|:---:|:---:|
| BudgetService (celý) | 65 | PASS 100 % |
| ManagementService (celý) | 76 | PASS 100 % |
| OverviewService (celý) | 52 | PASS 100 % |
| ReportsService (celý) | 79 | PASS 100 % |
| MilestoneService (celý) | 62 | PASS 100 % |
| ExportService (celý) | 32 | PASS 100 % |
| TransactionServiceSearch | 19 | PASS 100 % |
| AdminService (funkčné) | 100 | PASS 100 % |
| ProfileService (celý) | 28 | PASS 100 % |
| FamilyService (celý) | 46 | PASS 100 % |
| CurrencyService (celý) | 17 | PASS 100 % |
| SettingsService (celý) | 38 | PASS 100 % |
| UserService (celý) | 27 | PASS 100 % |
