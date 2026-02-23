# Šporixx
Desktopová aplikácia na správu osobných financií.
Vyvinutá v rámci predmetu VAVA 2026, STU FIIT.

---

## Nastavenie projektu

### Požiadavky
- Java SE JDK 25
- IntelliJ IDEA Ultimate
- Maven (zabudovaný v IntelliJ)

---

## Vetvy (Branch stratégia)

| Vetva | Účel |
|---|---|
| `main` | Finálna, odovzdaná verzia |
| `develop` | Hlavná vývojová vetva |
| `feature/xxx` | Vývoj konkrétnej funkcie |

**Nikto nepushuje priamo na `main` ani `develop`.**
Každá zmena ide cez Pull Request.

---

## Pravidlá code review

### Pred otvorením Pull Requestu
- Kód musí byť otestovaný lokálne – aplikácia nesmie padať
- Názov PR musí jasne popisovať čo robí, napr. `Pridanie JDBC pre transakcie`
- PR smeruje vždy do `develop`, nikdy priamo do `main`
- Jeden PR = jedna funkcionalita, nemiešaj viac vecí dokopy

### Počas code review
- Každý PR musí schváliť aspoň **1 člen tímu** pred mergnutím
- Reviewer kontroluje:
  - Či kód funguje a dáva zmysel
  - Či sú dodržané konvencie pomenovania (camelCase pre premenné, PascalCase pre triedy)
  - Či nie sú public polia – všetko cez gettery/settery (alebo Lombok)
  - Či je prítomné logovanie pri dôležitých akciách
  - Či nie sú v kóde SQL injekcie (používať prepared statements)
  - Či nie je zbytočne duplicitný kód (DRY princíp)

### Komentáre v PR
- Komentáre píš konštruktívne – nie "toto je zle" ale "navrhoval/a by som to urobiť takto: ..."
- Autor PR musí reagovať na každý komentár – buď opraviť alebo vysvetliť prečo nie
- PR sa merguje až keď sú všetky komentáre vyriešené

### Po mergnutí
- Feature vetva sa zmaže
- Autor skontroluje že `develop` funguje správne po merge

---

## Konvencie kódu

- Jazyk kódu: **angličtina** (názvy tried, metód, premenných)
- Komentáre: slovenčina alebo angličtina
- Balíčky: `sk.stuba.fiit.sporixx.nazovvrstvy`
- Na modelové triedy používať **Lombok** (@Data, @Builder...)
- Každá vrstva v samostatnom balíčku – GUI nesmie pristupovať priamo na DB
