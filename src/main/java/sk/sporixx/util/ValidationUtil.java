package sk.sporixx.util;

import java.util.regex.Pattern;

/**
 * Centralizovaná validácia vstupov pre service vrstvu.
 * Všetky regex Pattern-y sú predkompilované (Pattern.compile).
 */
public final class ValidationUtil {

    /** Email regex*/
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );

    /**
     * Celé meno (meno + priezvisko v jednom poli).
     * Musí obsahovať aspoň 2 slová oddelené medzerou.
     * Každé slovo: písmená (aj diakritika).
     * Povolené: medzery, pomlčky, apostrofy.
     * Celkovo 3-100 znakov.
     * OK:  "Adela Kudláčová", "Anna-Mária Nová", "Ján O'Brien Veľký"
     * ZLE: "Adela", "123 456"
     */
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile(
            "^\\p{L}[\\p{L}'-]*(?:\\s+\\p{L}[\\p{L}'-]*)+$"
    );

    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null;
    }

    /**
     * Validuje celé meno.
     * Musí obsahovať aspoň 2 slová.
     *
     * @param fullName celé meno
     * @return true ak je platné
     */
    public static boolean isValidFullName(String fullName) {
        if (fullName == null) {return false;}

        String trimmed = fullName.trim();
        int length = trimmed.length();

        if (length < 3 || length > 300) {
            return false;
        }
        return FULL_NAME_PATTERN.matcher(fullName.trim()).matches();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

