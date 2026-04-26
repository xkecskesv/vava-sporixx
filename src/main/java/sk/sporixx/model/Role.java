package sk.sporixx.model;

import lombok.Getter;

/**
 * Roly používateľov.
 * Mapovanie na account_access.access_level v DB:
 *   1 = USER (bežný používateľ)
 *   2 = FAMILY_MANAGER (manažér rodiny / power user)
 *   3 = ADMIN (administrátor)
 */
@Getter
public enum Role {

    USER(1, "User"),
    FAMILY_MANAGER(2, "Family Manager"),
    ADMIN(3, "Admin");

    private final int accessLevel;
    private final String displayName;

    Role(int accessLevel, String displayName) {
        this.accessLevel = accessLevel;
        this.displayName = displayName;
    }

    /**
     * Konvertuje access_level z DB na Role enum.
     * @param accessLevel hodnota z account_access.access_level
     * @return zodpovedajúca Role, defaultne USER
     */
    public static Role fromAccessLevel(int accessLevel) {
        for (Role role : values()) {
            if (role.accessLevel == accessLevel) {
                return role;
            }
        }
        return USER; //default
    }
}

