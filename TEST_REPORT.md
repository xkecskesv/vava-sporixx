# Unit Test Report - Sporixx

**Dátum:** 28. apríl 2026
**Celkový čas:** 7:55 min  
**Java:** OpenJDK 25 (loom)  
**Framework:** JUnit Jupiter 5.11.3 / Maven Surefire 3.2.5

---

## Celkové výsledky

| Celkom testov | Prešlo  | Zlyhalo | Chyby | Preskočené |
|:---:|:-------:|:-------:|:-------:|:----------:|
| **540** | **529** |  **5**  |  **6**  |   **0**    |

> Úspešnosť: **97,96 %**

---

## Výsledky podľa testovacích tried

### AdminService

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:----:|:----:|:-----:|---:|
| `AdminServiceAuthorizationTest$AsFamilyManager` | 7 |  7   |  0   |   0   | 4,907 s |
| `AdminServiceAuthorizationTest$AsRegularUser` | 7 |  7   |  0   |   0   | 5,865 s |
| `AdminServiceAuthorizationTest$NotLoggedIn` | 6 |  6   |  0   |   0   | 4,885 s |
| `AdminServiceChangeOwnPasswordTest` | 12 |  12  |  0   |   0   | 13,57 s |
| `AdminServiceGetAllUsersTest` | 13 |  13  |  0   |   0   | 10,89 s |
| `AdminServiceIntegrationTest` | 11 |  11  |  0   |   0   | 11,66 s |
| `AdminServiceLifecycleTest` | 26 |  26  |  0   |   0   | 22,17 s |
| `AdminServiceUpdateUserTest` | 25 |  25  |  0   |   0   | 21,16 s |
| `AdminServicePerformanceTest$ChangePasswordPerformance` | 1 |  0   |  0   |   1   | 82,24 s |
| `AdminServicePerformanceTest$GetAllUsersPerformance` | 2 |  1   |  0   |   1   | 137,8 s |
| `AdminServicePerformanceTest$LifecyclePerformance` | 1 |  0   |  0   |   1   | 55,20 s |
| `AdminServicePerformanceTest$UpdateUserPerformance` | 1 |  0   |  0   |   1   | 84,63 s |

---

### AuthService

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:-----:|---:|
| `AuthServiceTest` | 35 | 31 | 2 |   2   | 17,39 s |
| `AuthServicePerformanceTest$LoginPerformance` | 2 | 1 | 0 |   1   | 3,758 s |
| `AuthServicePerformanceTest$LogoutPerformance` | 1 | 0 | 0 |   1   | 1,251 s |
| `AuthServicePerformanceTest$RegisterPerformance` | 1 | 0 | 0 |   1   | 0,289 s |

---

### BudgetService

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `BudgetServiceCustomAllocationTest` | 17 | 17 | 0 | 0 | 0,004 s |
| `BudgetServiceLoadDataTest` | 17 | 17 | 0 | 0 | 0,012 s |
| `BudgetServiceSaveSetupTest` | 23 | 23 | 0 | 0 | 0,008 s |
| `BudgetServiceScenarioTest` | 8 | 8 | 0 | 0 | 0,009 s |
| `BudgetServicePerformanceTest$CustomAllocationPerformance` | 1 | 1 | 0 | 0 | 0,008 s |
| `BudgetServicePerformanceTest$SavePerformance` | 1 | 1 | 0 | 0 | 0,005 s |
| `BudgetServicePerformanceTest$LoadPerformance` | 2 | 2 | 0 | 0 | 0,033 s |

---

### ManagementService - Účty (Accounts)

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `ManagementAccountServiceTest$CreatePrivateAccount` | 7 | 7 | 0 | 0 | 0,005 s |
| `ManagementAccountServiceTest$CreateSavingAccount` | 9 | 9 | 0 | 0 | 0,004 s |
| `ManagementAccountServiceTest$DeleteAccount` | 4 | 4 | 0 | 0 | 0,007 s |
| `ManagementAccountServiceTest$GetSavingGoal` | 2 | 2 | 0 | 0 | 0,004 s |
| `ManagementAccountServiceTest$UpdateAccountDescription` | 3 | 3 | 0 | 0 | 0,004 s |
| `ManagementAccountServiceTest$UpdateSavingAccount` | 5 | 5 | 0 | 0 | 0,008 s |

---

### ManagementService - Kategórie (Categories)

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `ManagementCategoryServiceTest$AddCategory` | 6 | 6 | 0 | 0 | 0,006 s |
| `ManagementCategoryServiceTest$DeleteCategory` | 5 | 5 | 0 | 0 | 0,052 s |
| `ManagementCategoryServiceTest$GetCategories` | 5 | 5 | 0 | 0 | 0,004 s |
| `ManagementCategoryServiceTest$UpdateCategory` | 8 | 8 | 0 | 0 | 0,012 s |

---

### ManagementService - Opakujúce sa pravidlá (Recurring Rules)

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `ManagementRecurringRuleServiceTest$AddRuleValidation` | 11 | 11 | 0 | 0 | 0,003 s |
| `ManagementRecurringRuleServiceTest$DeleteRule` | 2 | 2 | 0 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$FrequencyTypes` | 6 | 6 | 0 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$GetRules` | 4 | 4 | 0 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$UpdateRule` | 3 | 3 | 0 | 0 | 0,001 s |
| `ManagementRecurringRuleServiceTest$UpdateRuleEdgeCases` | 3 | 3 | 0 | 0 | 0 s |

---

### ManagementService - Výkon (Performance)

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `ManagementPerformanceTest$AccountPerformance` | 4 | 4 | 0 | 0 | 0,019 s |
| `ManagementPerformanceTest$CategoryPerformance` | 5 | 5 | 0 | 0 | 0,037 s |
| `ManagementPerformanceTest$RecurringRulePerformance` | 3 | 3 | 0 | 0 | 0,007 s |

---

### OverviewService - Prehľad účtov

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `OverviewServiceAccountsSummaryTest$AccountList` | 3 | 3 | 0 | 0 | 0,001 s |
| `OverviewServiceAccountsSummaryTest$EdgeCases` | 3 | 3 | 0 | 0 | 0,002 s |
| `OverviewServiceAccountsSummaryTest$SavingGoals` | 5 | 5 | 0 | 0 | 0,002 s |
| `OverviewServiceAccountsSummaryTest$TotalBalance` | 3 | 3 | 0 | 0 | 0,002 s |

---

### OverviewService - Aktivity

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `OverviewServiceActivitiesTest$Combined` | 2 | 2 | 0 | 0 | 0 s |
| `OverviewServiceActivitiesTest$EdgeCases` | 4 | 4 | 0 | 0 | 0,005 s |
| `OverviewServiceActivitiesTest$EmptyData` | 1 | 1 | 0 | 0 | 0,001 s |
| `OverviewServiceActivitiesTest$RecentTransactions` | 6 | 6 | 0 | 0 | 0,003 s |
| `OverviewServiceActivitiesTest$UpcomingPayments` | 7 | 7 | 0 | 0 | 0,002 s |

---

### OverviewService - Analytika

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `OverviewServiceAnalyticsTest$AccountFiltering` | 2 | 2 | 0 | 0 | 0 s |
| `OverviewServiceAnalyticsTest$ChartPeriodMeta` | 3 | 3 | 0 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$EmptyData` | 2 | 2 | 0 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$GroupByDay` | 3 | 3 | 0 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$GroupByMonth` | 4 | 4 | 0 | 0 | 0,001 s |
| `OverviewServiceAnalyticsTest$TotalIncomeCalculation` | 3 | 3 | 0 | 0 | 0 s |

---

### OverviewService - Výkon (Performance)

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `OverviewPerformanceTest$AccountsSummaryPerformance` | 2 | 2 | 0 | 0 | 0,006 s |
| `OverviewPerformanceTest$ActivitiesPerformance` | 3 | 3 | 0 | 0 | 0,014 s |
| `OverviewPerformanceTest$AnalyticsPerformance` | 4 | 4 | 0 | 0 | 0,010 s |

---

### ReportsService

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `ReportsServiceCategoryExpenseTest` | 8 | 8 | 0 | 0 | 0,002 s |
| `ReportsServiceCategoryExpenseJdbcTest` | 9 | 9 | 0 | 0 | 0,140 s |
| `ReportsServiceIncomeExpenseTest` | 10 | 10 | 0 | 0 | 0,009 s |
| `ReportsServiceIncomeExpenseJdbcTest` | 15 | 15 | 0 | 0 | 0,248 s |
| `ReportsServiceRecurringExpenseTest` | 4 | 4 | 0 | 0 | 0,003 s |
| `ReportsServiceRecurringExpenseJdbcTest` | 5 | 5 | 0 | 0 | 0,072 s |
| `ReportsServiceSavingAccountsTest` | 13 | 13 | 0 | 0 | 0,003 s |
| `ReportsServiceSavingAccountsJdbcTest` | 14 | 14 | 0 | 0 | 0,723 s |
| `ReportsServiceWantNeedTest` | 8 | 8 | 0 | 0 | 0,003 s |
| `ReportsServiceWantNeedJdbcTest` | 8 | 8 | 0 | 0 | 0,118 s |
| `ReportsServicePerformanceTest$CategoryExpensePerformance` | 1 | 1 | 0 | 0 | 0,003 s |
| `ReportsServicePerformanceTest$IncomeExpensePerformance` | 3 | 3 | 0 | 0 | 0,035 s |
| `ReportsServicePerformanceTest$SavingAccountsPerformance` | 1 | 1 | 0 | 0 | 0,001 s |
| `ReportsServicePerformanceTest$WantNeedPerformance` | 2 | 2 | 0 | 0 | 0,013 s |

---

### TransactionService

| Testovacia trieda | Testov | PASS | FAIL | Error | Čas |
|---|:---:|:---:|:---:|:---:|---:|
| `TransactionServiceAddTest` | 21 | 20 | 1 | 0 | 0,016 s |
| `TransactionServiceDeleteTest` | 9 | 8 | 1 | 0 | 0,006 s |
| `TransactionServiceUpdateTest` | 13 | 12 | 1 | 0 | 0,014 s |
| `TransactionServiceSearchTest` | 19 | 19 | 0 | 0 | 0,008 s |
| `TransactionServicePerformanceTest$AddPerformance` | 2 | 2 | 0 | 0 | 0,017 s |
| `TransactionServicePerformanceTest$DeletePerformance` | 1 | 1 | 0 | 0 | 0,022 s |
| `TransactionServicePerformanceTest$LoadPerformance` | 2 | 2 | 0 | 0 | 0,003 s |
| `TransactionServicePerformanceTest$SearchPerformance` | 1 | 1 | 0 | 0 | 0,019 s |

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

## Chyby (Errors)

### 1–4. `AdminServicePerformanceTest` - Timeouty

| Test | Limit | Skutočný čas |
|---|:---:|---:|
| `changePassword100x_withinTimeLimit` | 3 s | **82,24 s** |
| `with500Users_withinTimeLimit` | 2 s | **136,9 s** |
| `deactivateAndActivate200Users_withinTimeLimit` | 3 s | **55,20 s** |
| `update300Users_withinTimeLimit` | 2 s | **84,63 s** |

**Príčina:** Všetky 4 testy zlyhali na `java.util.concurrent.TimeoutException`. Operácie s používateľmi (zmena hesla pomocou BCrypt, hromadné aktualizácie) sú príliš pomalé - BCrypt hashing pri 100 zmenách hesla trvá rádovo desiatky sekúnd namiesto 3 sekúnd.

---

### 5. `AuthServiceTest.logout_shouldSucceed` - riadok 383
**Príčina:** `IllegalStateException: ServiceLocator not initialized!`  
**Popis:** Test volá `login()` pred `logout()`, no `ServiceLocator` nie je inicializovaný.

---

### 6. `AuthServiceTest.logout_withActiveSession_shouldLogAndClear` - riadok 397
**Príčina:** `IllegalStateException: ServiceLocator not initialized!`  
**Popis:** Rovnaký problém - závislost na `ServiceLocator` v `AuthServiceImpl.login()`.

---

## Analýza problémov

### Problém 1: `ServiceLocator` nie je inicializovaný v testoch
- **Dotknuté testy:** 4 testy v `AuthServiceTest`
- **Príčina:** `AuthServiceImpl.login()` volá `ServiceLocator.getSettingsService()`, čo vyžaduje `ServiceLocator.init()` - táto metóda sa bežne volá v `Main.start()`, ale v unit testoch sa nevolá.
- **Odporúčanie:** Vložiť `ServiceLocator.init(...)` do `@BeforeEach` / `@BeforeAll` v testoch, alebo refaktorovať `AuthServiceImpl` tak, aby `SettingsService` bol injektovaný cez konštruktor (dependency injection).

### Problém 2: Nesprávna logika transfer transakcií
- **Dotknuté testy:** 3 testy v `TransactionServiceAddTest`, `TransactionServiceDeleteTest`, `TransactionServiceUpdateTest`
- **Príčina:** Logika pre spracovanie oboch nôžok prevodu (transfer legs) obsahuje chyby:
  - Auto-kategória sa priradí aj pri prevodoch na non-saving účty.
  - Zmazanie jednej nôžky nesprávne neobnoví zostatok druhého účtu.
  - Aktualizácia jednej nôžky nesprávne mení zostatok iného účtu.
- **Odporúčanie:** Skontrolovať a opraviť metódy `add`, `delete` a `update` v `TransactionServiceImpl` pri práci s `TRANSFER` typom transakcií.

### Problém 3: Výkonnostné problémy AdminService
- **Dotknuté testy:** 4 testy v `AdminServicePerformanceTest`
- **Príčina:** BCrypt (cost=12) je zámerne pomalý - 100 zmien hesla trvá ~82 sekúnd. Pre performance testy je limit 2–3 sekundy nerealistický pri reálnom BCrypt hashovaní.
- **Odporúčanie:** 
  - Pre performance testy používať BCrypt s nižším cost faktorom (napr. cost=4).
  - Alebo navýšiť timeout limity na realistické hodnoty.
  - Alebo mockovať password hashing v performance testoch.

---

## Dobre pokryté oblasti

| Oblasť | Počet testov | Výsledok |
|---|:---:|:---:|
| BudgetService (celý) | 65 | PASS 100 % |
| ManagementService (celý) | 76 | PASS 100 % |
| OverviewService (celý) | 52 | PASS 100 % |
| ReportsService (celý) | 79 | PASS 100 % |
| TransactionServiceSearch | 19 | PASS 100 % |
| AdminService (funkčné) | 100 | PASS 100 % |



