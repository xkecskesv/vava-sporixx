package sk.sporixx.util;

import java.util.regex.Pattern;

/**
 * Centralizovaná validácia vstupov pre service vrstvu.
 * Všetky regex Pattern-y sú predkompilované (Pattern.compile).
 */
public final class ValidationUtil {

    /**
     * Email – stredne striktná validácia (bez verifikačného mailu).
     * Pravidlá:
     *  - lokálna časť: a-z, A-Z, 0-9, znaky . _ % + -  (max 64 znakov)
     *  - žiadne dve bodky za sebou, nezačína ani nekončí bodkou
     *  - doména: alfanumerické časti oddelené bodkami, pomlčky povolené vnútri
     *    (žiadna časť nezačína ani nekončí pomlčkou)
     *  - TLD: aspoň 2 písmená
     *  - celková max. dĺžka sa kontroluje v metóde (254 znakov)
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^(?![.\\-])" +                         // lokálna časť nezačína . alebo -
                    "(?!.*\\.\\.)(?!.*[.\\-]@)" +           // žiadne .., lokálna časť nekončí . alebo -
                    "[a-zA-Z0-9._%+\\-]{1,64}" +            // lokálna časť
                    "@" +
                    "[a-zA-Z0-9]" +                          // doména začína alfanumericky
                    "(?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?" + // stred domény (voliteľný, nekončí -)
                    "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*" + // subdomény
                    "\\.[a-zA-Z]{2,}$"                       // TLD
    );

    /**
     * Validuje meno alebo priezvisko (samostatné pole).
     * Pravidlá:
     *  - 1 až 2 slová oddelené jednou medzerou
     *  - každé slovo: písmená vrátane diakritiky, pomlčky, apostrofy
     *  - OK:  "Ján", "Anna-Mária", "Mary Jane"
     *  - ZLE: "Ján Pavol Peter", "123", ""
     */
    private static final Pattern NAME_PART_PATTERN = Pattern.compile(
            "^\\p{L}[\\p{L}']*(?:[-\\s]\\p{L}[\\p{L}']*)?$"
    );

    /**
     * Kontroluje či meno/priezvisko obsahuje len povolené znaky.
     * Povolené: písmená (vrátane diakritiky), medzery, pomlčky, apostrofy.
     * ZLE: číslice, špeciálne znaky (@, #, !, atď.)
     */
    private static final Pattern NAME_CHARACTERS_PATTERN = Pattern.compile(
            "^[\\p{L}\\s'\\-]+$"
    );

    /**
     * Normalizuje časť mena – oreže whitespace, zloží viaceré medzery na jednu.
     * "  Anna  Mária  " → "Anna Mária"
     */
    public static String normalizeName(String name) {
        if (name == null) return "";
        return name.trim()
                .replaceAll("\\s*-+\\s*", "-")   // viaceré pomlčky + okolité medzery - jedna pomlčka
                .replaceAll("\\s+", " ");          // viaceré medzery - jedna
    }

    /**
     * Validuje firstName alebo lastName – max 2 slová.
     */
    public static boolean isValidNamePart(String name) {
        if (name == null) return false;
        String normalized = normalizeName(name);
        if (normalized.isEmpty() || normalized.length() > 100) return false;
        return NAME_PART_PATTERN.matcher(normalized).matches();
    }

    public static boolean isValidNamePartCharacters(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return NAME_CHARACTERS_PATTERN.matcher(name.trim()).matches();
    }

    /** Heslo: minimálne 8 znakov */
    private static final int MIN_PASSWORD_LENGTH = 8;

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validuje emailovú adresu.
     * Kontroluje formát aj maximálnu dĺžku (254 znakov podľa RFC 5321).
     *
     * @param email emailová adresa
     * @return true ak je platná
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        if (trimmed.length() > 254) return false;
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

