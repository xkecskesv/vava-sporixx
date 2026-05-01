# Unit testy – Service vrstva (`sk.sporixx.service`)

Overview, management, milestone, export

Všetky testy sú **unit testy** – bežia bez databázy, bez JavaFX a bez siete.
Používajú **InMemory** implementácie repozitárov (z balíka `sk.sporixx.service.testovanie`)
a izolovaný `SessionManager` s testovacím používateľom.

Testy sú štruktúrované pomocou **JUnit 5 `@Nested`** tried pre prehľadnosť.

---

## Celkové výsledky

| Modul | Testovacích tried | Testov celkom | Errory | Čas behu |
|---|---|---|---|---|
| Overview – unit | 3 | 51 | 0 | ~0.1 s |
| Management – unit | 3 | 83 | 0 | ~0.1 s |
| Overview – performance | 1 | 9 | 0 | ~0.1 s |
| Management – performance | 1 | 12 | 0 | ~0.1 s |
| **Spolu** | **8** | **155** | **0** | **< 1 s** |

> Spustenie všetkých: `mvn test -Dtest="OverviewService*,Management*,OverviewPerformanceTest,ManagementPerformanceTest"`

---

## Overview modul

Testovaná trieda: `OverviewServiceImpl`

### `OverviewServiceAccountsSummaryTest` – loadAccountsSummary()

| @Nested skupina | Čo sa testuje | Testov |
|---|---|---|
| `TotalBalance` | Súčet zostatkov všetkých účtov; 1 účet = jeho zostatok; 2 účty = súčet | 3 |
| `AccountList` | Prázdna session → prázdny zoznam; počet účtov zodpovedá session | 3 |
| `SavingGoals` | Saving účet má goal; Main/Emergency nemajú goal; progress, stav goal | 5 |
| `EdgeCases` | Záporný zostatok; goal na 100%; počet účtov v session | 3 |
| **Spolu** | | **14** |

### `OverviewServiceActivitiesTest` – loadActivities()

| @Nested skupina | Čo sa testuje | Testov |
|---|---|---|
| `EmptyData` | Žiadne transakcie → prázdne zoznamy | 1 |
| `RecentTransactions` | Zoradenie od najnovšej; limit 5; len príjmy/len výdavky; filtrovanie podľa dátumu | 6 |
| `UpcomingPayments` | Len budúce platby; zoradenie podľa dátumu; limit; platby z viacerých účtov | 7 |
| `Combined` | Kombinácia posledných transakcií aj nadchádzajúcich platieb naraz | 2 |
| `EdgeCases` | Transakcia dnes; platba zajtra; zoradenie recent; zoradenie upcoming | 4 |
| **Spolu** | | **20** |

### `OverviewServiceAnalyticsTest` – loadAnalytics()

| @Nested skupina | Čo sa testuje | Testov |
|---|---|---|
| `TotalIncomeCalculation` | Len príjmy sa počítajú; len výdavky; kombinácia príjmov a výdavkov | 3 |
| `GroupByMonth` | Zoskupenie transakcií podľa mesiaca (SIX_MONTHS, TWELVE_MONTHS) | 4 |
| `GroupByDay` | Zoskupenie transakcií podľa dňa (ONE_MONTH, ONE_WEEK) | 3 |
| `EmptyData` | Žiadne transakcie → nulové hodnoty v grafe | 2 |
| `AccountFiltering` | Filtrovanie analytiky podľa konkrétneho accountId | 2 |
| `ChartPeriodMeta` | Správne metadáta grafu pre SIX_MONTHS (počet bodov, labely) | 3 |
| **Spolu** | | **17** |

---

## Management modul

### `ManagementCategoryServiceTest` – CategoryService

| @Nested skupina | Čo sa testuje | Testov |
|---|---|---|
| `GetCategories` | Prázdny repozitár; systémové kategórie; vlastné kategórie; kombinácia; abecedné zoradenie | 5 |
| `AddCategory` | Platný názov; prázdny názov; null názov; duplicita (case insensitive); duplicita voči systémovej; orezanie medzier | 6 |
| `UpdateCategory` | Platná aktualizácia; ochrana systémovej kategórie; prázdny nový názov; neznáme ID; duplicitný nový názov; self-update; cudzia kategória; trim medzier | 8 |
| `DeleteCategory` | Vymazanie vlastnej kategórie; ochrana systémovej; neznáme ID; kategória v použití; cudzia kategória | 5 |
| **Spolu** | | **24** |

### `ManagementRecurringRuleServiceTest` – RecurringRuleService

| @Nested skupina | Čo sa testuje | Testov |
|---|---|---|
| `GetRules` | Žiadne pravidlá; pridané pravidlá sa vrátia; pravidlá z viacerých účtov; deaktivované pravidlo sa nezobrazí | 4 |
| `AddRuleValidation` | Platný vstup; suma ≤ 0; záporná suma; prázdny popis; null popis; null klasifikácia; null startDate; interval ≤ 0; neznámy accountId; endDate pred startDate; endDate v minulosti | 11 |
| `FrequencyTypes` | DAILY, WEEKLY, YEARLY frekvencia; interval > 1; s endDate; WANT klasifikácia | 6 |
| `UpdateRule` | Platná aktualizácia; neznáme ID; záporná suma | 3 |
| `UpdateRuleEdgeCases` | Aktualizácia s budúcou endDate; prázdny popis; interval ≤ 0 | 3 |
| `DeleteRule` | Deaktivácia existujúceho pravidla; neznáme ID | 2 |
| **Spolu** | | **29** |

### `ManagementAccountServiceTest` – AccountService

| @Nested skupina | Čo sa testuje | Testov |
|---|---|---|
| `CreateSavingAccount` | Platné údaje → účet + goal; prázdny popis; targetAmount ≤ 0; target ≤ initial; dátum v minulosti; null dátum; záporný initial; duplicitný popis; nulový initial; pridanie do session | 10 |
| `CreatePrivateAccount` | Platné údaje; prázdny popis; záporná suma; duplicitný popis; nulový initial; pridanie do session; viac účtov s rôznymi popismi | 7 |
| `DeleteAccount` | Saving účet sa dá vymazať; Main Account – ochrana; Emergency Fund – ochrana; neznáme ID | 4 |
| `UpdateAccountDescription` | Platný popis; prázdny popis; neznáme ID | 3 |
| `UpdateSavingAccount` | Platná aktualizácia (popis + goal); prázdny popis; targetAmount ≤ 0; dátum v minulosti; non-saving účet | 5 |
| `GetSavingGoal` | Saving účet má aktívny goal; účet bez goal → Optional.empty() | 2 |
| **Spolu** | | **31** |

---

## Technické poznámky

- **Žiadna DB** – všetky repozitáre sú InMemory implementácie (List v pamäti)
- **Žiadny JavaFX** – testy nevyžadujú inicializáciu UI platformy
- **Izolácia** – každý test dostane čistý stav cez `@BeforeEach` / `@AfterEach`
- **SessionManager** – nastavuje sa s testovacím používateľom (id=1) a povinnými účtami (Main Account + Emergency Fund)
- **Rýchlosť** – celých 155 testov prebehne za menej ako 1 sekundu

---

## Performance testy

Overujú, že servisná vrstva zvládne veľké objemy dát v prijateľnom čase.
Každý test má pevný časový limit definovaný cez `@Timeout` (JUnit 5).
Výsledné časy sa vypisujú do konzoly s prefixom `[PERF]`.

### `ManagementPerformanceTest` (12 testov)

| @Nested skupina | Scenár | Objem | Limit |
|---|---|---|---|
| `CategoryPerformance` | Načítanie kategórií | 1 000 záznamov | 1 s |
| `CategoryPerformance` | Pridanie kategórií | 500 kategórií | 2 s |
| `CategoryPerformance` | Detekcia duplicity | 1 000 záznamov | 1 s |
| `CategoryPerformance` | Aktualizácia kategórie | 1 000 záznamov | 2 s |
| `CategoryPerformance` | Selectable kategórie | 1 000 záznamov | 1 s |
| `RecurringRulePerformance` | Pridanie pravidiel | 300 pravidiel | 3 s |
| `RecurringRulePerformance` | Načítanie pravidiel | 500 pravidiel | 2 s |
| `RecurringRulePerformance` | Deaktivácia pravidiel | 200 pravidiel | 2 s |
| `AccountPerformance` | Vytvorenie saving účtov | 100 účtov | 3 s |
| `AccountPerformance` | Vytvorenie private účtov | 100 účtov | 3 s |
| `AccountPerformance` | Vytvorenie + vymazanie | 100 účtov | 3 s |
| `AccountPerformance` | Načítanie saving goals | 100 účtov | 2 s |

### `OverviewPerformanceTest` (9 testov)

| @Nested skupina | Scenár | Objem | Limit |
|---|---|---|---|
| `AccountsSummaryPerformance` | loadAccountsSummary so saving goals | 500 goals | 1 s |
| `AccountsSummaryPerformance` | Opakované volanie loadAccountsSummary | 1 000 volaní | 1 s |
| `ActivitiesPerformance` | loadActivities s transakciami | 2 000 transakcií | 1 s |
| `ActivitiesPerformance` | loadActivities s recurring rules | 500 pravidiel | 1 s |
| `ActivitiesPerformance` | Opakované volanie loadActivities | 500 volaní | 2 s |
| `AnalyticsPerformance` | loadAnalytics ONE_MONTH | 5 000 transakcií | 1 s |
| `AnalyticsPerformance` | loadAnalytics TWELVE_MONTHS | 3 000 transakcií | 1 s |
| `AnalyticsPerformance` | loadAnalytics všetky ChartPeriod | 500 transakcií/periodu | 2 s |
| `AnalyticsPerformance` | Opakované volanie loadAnalytics | 200 volaní | 2 s |

### Namerané výsledky (referenčný beh)

| Test | Nameraný čas |
|---|---|
| addCategory x500 | 18 ms |
| getCategories (1 000) | < 1 ms |
| duplicateCheck (1 000) | 2 ms |
| updateCategory (1 000) | 1 ms |
| addRecurringRule x300 | 3 ms |
| deleteRecurringRule x200 | 3 ms |
| getRecurringRules (500) | < 1 ms |
| createSavingAccount x100 | 11 ms |
| createPrivateAccount x100 | 3 ms |
| deleteAccount x100 | 1 ms |
| getSavingGoal x100 | < 1 ms |
| loadAnalytics ONE_MONTH (5 000) | 1 ms |
| loadAnalytics TWELVE_MONTHS (3 000) | 1 ms |
| loadAnalytics všetky periody | 9 ms |
| loadAnalytics x200 volaní | 8 ms |
| loadActivities x500 volaní | 17 ms |
| loadAccountsSummary x1000 volaní | 12 ms |

