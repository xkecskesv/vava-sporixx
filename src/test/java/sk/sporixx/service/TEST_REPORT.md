# Unit Test Report - Sporixx

**Dátum:** 2. máj 2026
**Celkový čas:** 11 m 20 s
**Java:** OpenJDK 25 (loom)
**Framework:** JUnit Jupiter 5.11.3 / Maven Surefire 3.2.5

---

## Celkové výsledky

| Celkom testov | Prešlo  | Zlyhalo | Preskočené |
|:---:|:-------:|:-------:|:----------:|
| **819** | **819** |  **0**  |   **0**    |

> Úspešnosť: **100 %**

---

## Výsledky podľa testovacích tried

### AdminService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:----:|:----:|---:|
| `AdminServiceAuthorizationTest` | 20 | 20 | 0 | 52,48 s |
| `AdminServiceChangeOwnPasswordTest` | 12 | 12 | 0 | 44,80 s |
| `AdminServiceGetAllUsersTest` | 13 | 13 | 0 | 35,11 s |
| `AdminServiceIntegrationTest` | 11 | 11 | 0 | 38,33 s |
| `AdminServiceLifecycleTest` | 26 | 26 | 0 | 1 m 12 s |
| `AdminServicePerformanceTest` | 7 | 7 | 0 | 33,51 s |
| `AdminServiceUpdateUserTest` | 25 | 25 | 0 | 1 m 10 s |

---

### AuthService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `AuthServiceTest` | 35 | 35 | 0 | 56,67 s |

---

### BudgetService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `BudgetServiceCustomAllocationTest` | 17 | 17 | 0 | 24 ms |
| `BudgetServiceLoadDataTest` | 17 | 17 | 0 | 1 ms |
| `BudgetServiceSaveSetupTest` | 23 | 23 | 0 | 12 ms |
| `BudgetServiceScenarioTest` | 8 | 8 | 0 | 13 ms |
| `BudgetService – Performance testy` | 4 | 4 | 0 | 127 ms |

---

### ManagementService – Účty (AccountService)

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `AccountService – Management` | 30 | 30 | 0 | 20 ms |
| `Management – Performance testy` | 12 | 12 | 0 | 276 ms |

---

### ManagementService – Kategórie (CategoryService)

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `CategoryService – Management` | 24 | 24 | 0 | 20 ms |

---

### ManagementService – Opakujúce sa pravidlá (RecurringRuleService)

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `RecurringRuleService – Management` | 29 | 29 | 0 | 10 ms |

---

### OverviewService – Prehľad účtov

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `OverviewService – loadAccountsSummary()` | 14 | 14 | 0 | 15 ms |
| `OverviewService – loadActivities()` | 20 | 20 | 0 | 41 ms |
| `OverviewService – loadAnalytics()` | 17 | 17 | 0 | 243 ms |
| `Overview – Performance testy` | 9 | 9 | 0 | 135 ms |

---

### ReportsService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ReportsServiceCategoryExpenseTest` | 8 | 8 | 0 | < 1 ms |
| `ReportsServiceCategoryExpenseJdbcTest` | 9 | 9 | 0 | 1,57 s |
| `ReportsServiceIncomeExpenseTest` | 10 | 10 | 0 | 2 ms |
| `ReportsServiceIncomeExpenseJdbcTest` | 15 | 15 | 0 | 2,50 s |
| `ReportsServiceRecurringExpenseTest` | 4 | 4 | 0 | < 1 ms |
| `ReportsServiceRecurringExpenseJdbcTest` | 5 | 5 | 0 | 1,08 s |
| `ReportsServiceSavingAccountsTest` | 13 | 13 | 0 | 54 ms |
| `ReportsServiceSavingAccountsJdbcTest` | 14 | 14 | 0 | 2,69 s |
| `ReportsServiceWantNeedTest` | 8 | 8 | 0 | 5 ms |
| `ReportsServiceWantNeedJdbcTest` | 8 | 8 | 0 | 1,19 s |
| `ReportsService – Performance testy` | 7 | 7 | 0 | 150 ms |

---

### TransactionService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `TransactionServiceAddTest` | 21 | 21 | 0 | 42 ms |
| `TransactionServiceDeleteTest` | 9 | 9 | 0 | 9 ms |
| `TransactionServiceUpdateTest` | 13 | 13 | 0 | 50 ms |
| `TransactionServiceSearchTest` | 19 | 19 | 0 | 10 ms |
| `TransactionService – Performance testy` | 6 | 6 | 0 | 75 ms |

---

### ExportService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ExportService – exportIncomeExpenseToXml()` | 15 | 15 | 0 | 89 ms |
| `ExportService – exportSavingAccountsToXml()` | 17 | 17 | 0 | 331 ms |
| `ExportService – Performance testy` | 4 | 4 | 0 | 677 ms |

---

### ImportService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ImportService – importSavingAccountsFromXml` | 15 | 15 | 0 | 13,67 s |

---

### MilestoneService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `MilestoneService – Budget Keeper` | 13 | 13 | 0 | 1 ms |
| `MilestoneService – Saving Master` | 10 | 10 | 0 | < 1 ms |
| `MilestoneService – Smart Spender` | 13 | 13 | 0 | 17 ms |
| `MilestoneService – Investor` | 11 | 11 | 0 | 3 ms |
| `MilestoneService – Financial Title Key` | 15 | 15 | 0 | 78 ms |
| `MilestoneService – Performance testy` | 8 | 8 | 0 | 145 ms |

---

### ProfileService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `ProfileService – updateProfile` | 12 | 12 | 0 | 32,91 s |
| `ProfileService – changePassword` | 6 | 6 | 0 | 23,18 s |
| `ProfileService – foto a pohlavie` | 10 | 10 | 0 | 27,22 s |

---

### FamilyService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `FamilyService – getFamilyMembers` | 6 | 6 | 0 | 21,41 s |
| `FamilyService – sendFamilyRequest` | 10 | 10 | 0 | 37,61 s |
| `FamilyService – accept/rejectFamilyRequest` | 9 | 9 | 0 | 29,70 s |
| `FamilyService – remove, pendingRequests, sentRequests` | 10 | 10 | 0 | 35,66 s |
| `FamilyService – updateChildSavingAccount` | 11 | 11 | 0 | 38,73 s |

---

### CurrencyService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `CurrencyService` | 17 | 17 | 0 | 79 ms |

---

### SettingsService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `SettingsService` | 38 | 38 | 0 | 23 ms |

---

### UserService

| Testovacia trieda | Testov | PASS | FAIL | Čas |
|---|:---:|:---:|:---:|---:|
| `UserService` | 27 | 27 | 0 | 5,35 s |

---

## Dobre pokryté oblasti

| Oblasť | Počet testov | Výsledok |
|---|:---:|:---:|
| BudgetService (celý) | 69 | PASS 100 % |
| ManagementService (celý) | 95 | PASS 100 % |
| OverviewService (celý) | 60 | PASS 100 % |
| ReportsService (celý) | 101 | PASS 100 % |
| MilestoneService (celý) | 70 | PASS 100 % |
| ExportService (celý) | 36 | PASS 100 % |
| ImportService (celý) | 15 | PASS 100 % |
| TransactionService (celý) | 68 | PASS 100 % |
| AdminService (celý) | 114 | PASS 100 % |
| AuthService (celý) | 35 | PASS 100 % |
| ProfileService (celý) | 28 | PASS 100 % |
| FamilyService (celý) | 46 | PASS 100 % |
| CurrencyService (celý) | 17 | PASS 100 % |
| SettingsService (celý) | 38 | PASS 100 % |
| UserService (celý) | 27 | PASS 100 % |
