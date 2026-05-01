# Výsledky výkonnostných (Performance) testov – Sporixx

> Dátum posledného spustenia: **2026-04-28**  
> Celkový výsledok: **39 testov prešlo, 4 zlyhali**

---

## Príkaz na spustenie všetkých performance testov

```bash
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -f pom.xml test \
  -Dtest="OverviewPerformanceTest,ManagementPerformanceTest,AdminServicePerformanceTest,BudgetServicePerformanceTest,TransactionServicePerformanceTest,ReportsServicePerformanceTest"
```

---

## Prehľad výsledkov

| Test trieda | Testy | Prešlo | Zlyhalo | Status |
|---|---|---|---|---|
| `BudgetServicePerformanceTest` | 4 | 4 | 0 | ✅ |
| `OverviewPerformanceTest` | 9 | 9 | 0 | ✅ |
| `ManagementPerformanceTest` | 12 | 12 | 0 | ✅ |
| `TransactionServicePerformanceTest` | 6 | 6 | 0 | ✅ |
| `ReportsServicePerformanceTest` | 7 | 7 | 0 | ✅ |
| `AdminServicePerformanceTest` | 5 | 1 | 4 | ❌ |

---

## BudgetService – Performance testy

Všetky testy prešli výrazne pod časovým limitom. BudgetService je vysoko výkonný aj pri veľkých objemoch dát.

| Test | Operácia | Nameraný čas | Limit | Výsledok |
|---|---|---|---|---|
| `saveCustomAllocation500x_withinTimeLimit` | Uloženie custom alokácie 500× | **8 ms** | 2 000 ms | ✅ |
| `saveBudgetSetup500x_withinTimeLimit` | Uloženie budget setup 500× | **5 ms** | 2 000 ms | ✅ |
| `loadBudgetData1000x_withinTimeLimit` | Načítanie budget dát 1 000× | **29 ms** | 1 000 ms | ✅ |
| `loadBudgetDataWith2000Transactions_withinTimeLimit` | Načítanie s 2 000 emergency transakciami | **3 ms** | 2 000 ms | ✅ |

**Záver:** BudgetService zvláda opakované čítania aj zápisy s extrémne nízkou latenciou. Najnáročnejšia operácia (`loadBudgetData x1000`) trvala len 29 ms teda iba 3 % z povoleného limitu.

---

## Overview – Performance testy

Všetky testy prešli. Overview modul efektívne spracúva veľké množstvá saving goals, transakcií aj recurring pravidiel.

### AccountsSummaryPerformance (2 testy, celkový čas: 23 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `summaryWith500Goals_withinTimeLimit` | `loadAccountsSummary()` s 500 saving goals | 1 000 ms | ✅ |
| `summaryCalledRepeatedly_withinTimeLimit` | `loadAccountsSummary()` volaná 1 000× | 1 000 ms | ✅ |

### ActivitiesPerformance (3 testy, celkový čas: 14 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `activitiesWith2000Transactions_withinTimeLimit` | `loadActivities()` s 2 000 transakciami | 1 000 ms | ✅ |
| `activitiesWith500RecurringRules_withinTimeLimit` | `loadActivities()` s 500 recurring rules | 1 000 ms | ✅ |
| `activitiesCalledRepeatedly_withinTimeLimit` | `loadActivities()` volaná 500× | 2 000 ms | ✅ |

### AnalyticsPerformance (4 testy, celkový čas: 30 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `analyticsWith5000Transactions_oneMonth_withinTimeLimit` | `loadAnalytics()` s 5 000 transakciami (ONE_MONTH) | 1 000 ms | ✅ |
| Ďalšie 3 analytics testy | Rôzne obdobia a objemy | rôzne | ✅ |

**Záver:** Overview operácie sú extrémne rýchle. Celý balík 9 testov sa spustil za menej ako 30 ms.

---

## Management – Performance testy

Všetky testy prešli. Management modul (kategórie, recurring rules, účty) zvláda veľké objemy bez problémov.

### CategoryPerformance (5 testov, celkový čas: 30 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `load1000Categories_withinTimeLimit` | Načítanie 1 000 kategórií | 1 000 ms | ✅ |
| `add500Categories_withinTimeLimit` | Pridanie 500 kategórií | 2 000 ms | ✅ |
| `duplicateCheckIn1000Categories_withinTimeLimit` | Detekcia duplicity v 1 000 kategóriách | 1 000 ms | ✅ |
| `updateCategoryIn1000_withinTimeLimit` | Aktualizácia kategórie v 1 000 záznamoch | 2 000 ms | ✅ |
| `getSelectableCategories1000_withinTimeLimit` | Načítanie selectable kategórií z 1 000 | 1 000 ms | ✅ |

### RecurringRulePerformance (3 testy, celkový čas: 12 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `add300Rules_withinTimeLimit` | Pridanie 300 pravidiel | 3 000 ms | ✅ |
| Ďalšie 2 testy | Načítanie a mazanie pravidiel | rôzne | ✅ |

### AccountPerformance (4 testy, celkový čas: 43 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| Vytvorenie 200 účtov (saving + private) | AccountService operácie | rôzne | ✅ |

**Záver:** Management operácie sú efektívne aj pri tisíckach záznamov. Celý balík 12 testov prebehol za 43 ms.

---

## TransactionService – Performance testy

Všetky testy prešli. TransactionService zvláda veľké objemy transakcií pri všetkých CRUD operáciách.

### AddPerformance (2 testy, celkový čas: 24 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `add500Incomes_withinTimeLimit` | Pridanie 500 príjmov | 3 000 ms | ✅ |
| `add500Expenses_withinTimeLimit` | Pridanie 500 výdavkov | 3 000 ms | ✅ |

### LoadPerformance (2 testy, celkový čas: 5 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `load1000Transactions_withinTimeLimit` | Načítanie 1 000 transakcií | 2 000 ms | ✅ |
| `getAllTransactions_withinTimeLimit` | `getAllTransactions()` naprieč 3 účtami (1 000 tx) | 2 000 ms | ✅ |

### DeletePerformance (1 test, celkový čas: 34 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `delete300Transactions_withinTimeLimit` | Vymazanie 300 transakcií | 3 000 ms | ✅ |

### SearchPerformance (1 test, celkový čas: 59 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `search1000Transactions_withinTimeLimit` | Vyhľadávanie naprieč 1 000 transakciami | 2 000 ms | ✅ |

**Záver:** TransactionService je výkonný vo všetkých operáciách. Vyhľadávanie (59 ms) je najpomalšia operácia, no stále výrazne pod limitom.

---

## ReportsService – Performance testy

Všetky testy prešli. ReportsService správne a rýchlo agreguje dáta z veľkých objemov transakcií.

### IncomeExpensePerformance (3 testy, celkový čas: 51 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `with3000Transactions_withinTimeLimit` | `loadIncomeExpenseData()` s 3 000 transakciami | 1 000 ms | ✅ |
| `allPeriods_withinTimeLimit` | Všetky ChartPeriod hodnoty s 500 tx | 2 000 ms | ✅ |
| `called500x_withinTimeLimit` | `loadIncomeExpenseData()` volaná 500× | 2 000 ms | ✅ |

### CategoryExpensePerformance (1 test, celkový čas: 15 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `with2000Expenses_withinTimeLimit` | `loadCategoryExpenseData()` s 2 000 výdavkami | 1 000 ms | ✅ |

### WantNeedPerformance (2 testy, celkový čas: 33 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `with2000Expenses_withinTimeLimit` | `loadWantNeedData()` s 2 000 výdavkami | 1 000 ms | ✅ |
| `called500x_withinTimeLimit` | `loadWantNeedData()` volaná 500× | 2 000 ms | ✅ |

### SavingAccountsPerformance (1 test, celkový čas: 7 ms)

| Test | Operácia | Limit | Výsledok |
|---|---|---|---|
| `with500Goals_withinTimeLimit` | `loadSavingAccountsData()` s 500 saving goals | 1 000 ms | ✅ |

**Záver:** ReportsService agreguje dáta veľmi efektívne. Najnáročnejší balík (IncomeExpensePerformance) prebehol za 51 ms pri 3 rôznych scenároch.

---

## AdminService – Performance testy (4 zlyhania)

AdminService mal vážne problémy s výkonom. Štyri testy presiahli povolený časový limit – niektoré dramaticky.

| Test | Operácia | Nameraný čas   | Limit | Výsledok |
|---|---|----------------|---|---|
| `with500Users_withinTimeLimit` | `getAllUsers()` so 500 používateľmi | **136 600 ms** | 2 000 ms | ❌ Timeout |
| `called1000x_withinTimeLimit` | `getAllUsers()` volaná 1 000× | **1 930 ms**   | 2 000 ms | ✅ |
| `update300Users_withinTimeLimit` | `updateUser()` pre 300 používateľov | **86 060 ms**  | 2 000 ms | ❌ Timeout |
| `deactivateAndActivate200Users_withinTimeLimit` | Deaktivácia + aktivácia 200 používateľov | **56 560 ms**  | 3 000 ms | ❌ Timeout |
| `changePassword100x_withinTimeLimit` | Zmena hesla 100× | **82 280 ms**  | 3 000 ms | ❌ Timeout |

**Príčina zlyhaní:**  
Operácie s veľkým počtom používateľov (`getAllUsers` so 500 záznamami, `updateUser`, `deactivateUser`/`activateUser`, `changeOwnPassword`) sú výrazne pomalšie ako sa očakávalo. Skutočný čas bol desiatky až stokrát vyšší ako povolený limit. Pravdepodobná príčina je chýbajúci index v databáze alebo ineffektívne SQL dopyty pri väčšom počte záznamov (N+1 problém).

> Poznámka: Test `getAllUsers() sa volá 1 000-krát` prešiel to naznačuje, že problém je pri **načítaní a spracovaní 500 riadkov naraz**, nie pri opakovanom volaní s malým počtom záznamov.

---

## Záver a odporúčania

### Čo funguje dobre
- **BudgetService, OverviewService, ManagementService, TransactionService, ReportsService**  všetky prešli výkonnostnými testmi s výraznou rezervou. Aplikačná logika pre správu rozpočtu, transakcií a reportov je efektívna.

### Čo treba opraviť

1. **AdminService – výkonnostný problém**
   - Operácie `getAllUsers()`, `updateUser()`, `deactivateUser()`/`activateUser()` a `changeOwnPassword()` sú príliš pomalé pri väčšom počte používateľov.
   - Odporúčanie: Skontrolovať SQL dopyty, pridať indexy na stĺpce `users.email`, `users.active`, a prípadne optimalizovať bcrypt iterácie pri zmene hesla.


