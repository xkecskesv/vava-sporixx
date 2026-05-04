package sk.sporixx.util;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilita pre hashovanie a overovanie hesiel pomocou BCrypt.
 * Používa knižnicu jBCrypt (org.mindrot:jbcrypt).
 */
public final class PasswordUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtil.class);

    /**
     * Cost factor pre BCrypt (2^12 = 4096 iterácií).
     * Hodnota 12 je dobrý kompromis medzi bezpečnosťou a výkonom.
     * Zvýšenie o 1 zdvojnásobí čas hashovania.
     */
    private static final int BCRYPT_COST = 12;

    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        logger.debug("Hashing password with BCrypt (cost={})", BCRYPT_COST);
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Overí, či zadané heslo zodpovedá uloženému BCrypt hashu.
     * @param plainPassword heslo v čistom texte na overenie
     * @param hashedPassword uložený BCrypt hash z databázy
     * @return true ak heslo zodpovedá hashu, false inak
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            logger.warn("Password verification failed: empty password");
            return false;
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            logger.warn("Password verification failed: empty hash");
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid BCrypt hash format: {}", e.getMessage());
            return false;
        }
    }

}
