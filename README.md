# Architektura

## 3-vrstvova architektura

### 1) UI vrstva (Presentation)
- Obsahuje obrazovky, formulare, tlacidla, tabulky a obsluhu udalosti (kliknutia)
- Zobrazuje data pouzivatelovi a zbiera jeho vstupy
- Neriesi databazu ani biznis pravidla - posiela poziadavky do Service vrstvy

### 2) Service vrstva (Business Logic)
- Obsahuje biznis logiku aplikacie: pravidla, vypocty, spracovanie operacii a logicke validacie
- Koordinuje operacie medzi UI a Repository vrstvou
- Vola Repository vrstvu pre citanie a ukladanie dat

### 3) Repository vrstva (Data Access)
- Obsahuje pristup k datam a databaze cez JDBC/SQL
- Riesi CRUD operacie a dotazy do databazy
- Repository vrstva neriesi UI ani biznis logiku

---

## Pravidla komunikacie medzi vrstvami

### Povolene
- UI -> Service: OK
- Service -> Repository: OK
- Repository -> DB: OK
- Navrat vysledkov ide naspat cez Repository -> Service -> UI

### Zakazane
- UI -> Repository: NIE
- UI -> DB: NIE
- Repository -> Service: NIE
- Repository -> UI: NIE

Povoleny tok volani je iba:
`UI -> Service -> Repository -> DB`

---

## Balicky

Zakladny root package:
`sk.sporixx`

### Core balicky
- `sk.sporixx.ui`
  UI vrstva - obrazovky, controllery a obsluha udalosti

- `sk.sporixx.service`
  Service vrstva - biznis logika, pravidla, vypocty, workflow

- `sk.sporixx.repository`
  Repository vrstva - JDBC/SQL pristup do DB (CRUD, dotazy)

- `sk.sporixx.model`
  Model vrstva - datove objekty (napr. Transaction, Budget, Goal, User, Category) bez GUI a bez DB logiky

### Support balicky
- `sk.sporixx.util`
  Pomocne utility a spolocne helpery

---

## Uz vytvorene modely

- `Transaction`
- `Budget`
- `Goal`
- `User`
- `Category`

Zatial jednoduche datove objekty, ktore budu neskor rozsirovane o dalsie atributy podla potrieb aplikacie- dohodnutie na meetingu

---

## Vytvorene Service interfacy

- `TransactionService`
- `BudgetService`
- `GoalService`
- `StatisticsService`
- `LoanCalculatorService`

Tieto interfacy predstavuju rozhranie biznis vrstvy, ktore bude volat UI vrstva

---

## Aktualne vytvorene Repository interfacy

- `TransactionRepository`
- `UserRepository`
- `BudgetRepository`
- `GoalRepository`
- `CategoryRepository`

Tieto interfacy predstavuju rozhranie datovej vrstvy, ktore bude volat Service vrstva

---

## Priklad fungovania - pridanie transakcie cez vrstvy

Pouzivatel prida vydavok "Jedlo" 10 EUR.

1) UI (`sk.sporixx.ui`)
- Pouzivatel vyplni formular a klikne na "Ulozit"
- UI vytvori objekt `Transaction`
- UI zavola:
  `transactionService.addTransaction(transaction)`

2) Service (`sk.sporixx.service`)
- Skontroluje pravidla (suma > 0 ...)
- Spracuje logiku operacie
- Zavola repository:
  `transactionRepository.save(transaction)`

3) Repository (`sk.sporixx.repository`)
- Vykona JDBC/SQL operaciu (napr. INSERT)
- Ulozi transakciu do databazy
- Vrati vysledok naspat do Service vrstvy

4) Model (`sk.sporixx.model`)
- `Transaction` je cisty datovy objekt, ktory sa prenasa medzi vrstvami