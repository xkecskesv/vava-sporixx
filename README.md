# Šporixx

Desktopová aplikácia na správu osobných financií.  
Vyvinutá v rámci predmetu **VAVA 2026**, STU FIIT.

---

# Návod na spustenie

## Systémové požiadavky

- **Java JDK 25**
- 64-bitový operačný systém (Windows, macOS, Linux)
- Minimálne rozlíšenie: 1024 × 700

Stiahnutie Java 25: https://jdk.java.net/25/

---

## Spustenie

Otvor terminál v priečinku kde sa nachádza JAR súbor a spusti:

```
java -jar sporixx-1.0-SNAPSHOT-jar-with-dependencies.jar
```

> Dvojklik na JAR súbor nemusí fungovať na všetkých systémoch — použi príkaz vyššie cez terminál.

---

## Časté problémy

---

### "UnsupportedClassVersionError" alebo aplikácia sa vôbec nespustí

**Príčina:** Máš nainštalovanú staršiu verziu Javy.

**Riešenie:**
1. Skontroluj verziu Javy: `java -version`
2. Ak zobrazuje verziu nižšiu ako 25, stiahni Java 25 z https://jdk.java.net/25/
3. Po inštalácii sa uisti že nová Java je nastavená v PATH

---

### Windows — "Windows protected your PC" (SmartScreen)

**Príčina:** JAR nie je podpísaný plateným certifikátom.

**Riešenie:** Klikni na **"More info"** → **"Run anyway"**

---

### Windows — aplikácia sa otvorí ale hneď spadne / neočakávaná chyba

**Príčina:** Nainštalovaných viac verzií Javy, systém používa nesprávnu.

**Riešenie:** Spusti explicitne s plnou cestou k Java 25:
```
"C:\Program Files\Java\jdk-25\bin\java.exe" -jar sporixx-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Uprav cestu podľa toho kde máš nainštalovaný JDK 25.

---

### macOS — "cannot be opened because it is from an unidentified developer"

**Riešenie (možnosť 1):** Pravý klik na JAR → **Otvoriť** → **Otvoriť** v dialógu.

**Riešenie (možnosť 2):** Prejdi do **Nastavenia systému → Súkromie a bezpečnosť** → scrolluj dole → klikni **"Otvoriť napriek tomu"** vedľa záznamu Sporixx.

---

### Umiestnenie databázy

Pri prvom spustení Sporixx automaticky vytvorí `sporixx.sqlite` v **rovnakom priečinku ako JAR**. Uisti sa že máš práva na zápis v danom priečinku.

Ak presunieš JAR do iného priečinka, databáza sa s ním nepresunie — vytvorí sa nová prázdna databáza.

---

### Predvolené prihlasovacie údaje admina

```
Email:    admin@sporixx.sk
Heslo:    Admin123!
```

> Po prvom prihlásení zmeň heslo admina.

---

# Autori

| Meno | GitHub |
|---|---|
| *David Babiš* | [@xbabis](https://github.com/xbabis) |
| *Matúš Béreš* | [@xberesm](https://github.com/xberesm) |
| *Štefan Čomor* | [@xcomor](https://github.com/xcomor) |
| *Adriana Kaňová* | [@Adriana603](https://github.com/Adriana603) |
| *Viktória Kecskés* | [@xkecskesv](https://github.com/xkecskesv) |
| *Adam Kubaliak* | [@KubaliakAdam](https://github.com/KubaliakAdam) |
| *Adela Kudláčová* | [@adelakudlacova](https://github.com/adelakudlacova) |
| *Marek Moško* | [@MrM4r3k](https://github.com/MrM4r3k) |
| *Adam Pašteka* | [@Tencobijemuchy](https://github.com/Tencobijemuchy) |
| *Marek Rakúš* | [@marekrakus03](https://github.com/marekrakus03) |
