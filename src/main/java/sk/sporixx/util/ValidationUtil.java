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
     * Celé meno (meno + priezvisko v jednom poli).
     * Musí obsahovať aspoň 2 slová oddelené medzerou.
     * Každé slovo: písmená (aj diakritika).
     * Povolené: medzery, pomlčky, apostrofy.
     * OK:  "Adela Kudláčová", "Anna-Mária Nová", "Ján O'Brien Veľký"
     * ZLE: "Adela", "123 456"
     */
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
            "^\\p{L}[\\p{L}'-]*(?:\\s+\\p{L}[\\p{L}'-]*)+$"
    );

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

    /**
     * Validuje celé meno.
     * Musí obsahovať aspoň 2 slová.
     *
     * @param fullName celé meno
     * @return true ak je platné
     */
    public static boolean isValidFullName(String fullName) {
        if (fullName == null) {
            return false;
        }

        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int length = trimmed.length();

        if (length < 3 || length > 300) {
            return false;
        }
        return FULL_NAME_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

