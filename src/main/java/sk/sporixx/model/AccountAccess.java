package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre prístup k účtu (väzba medzi používateľmi a účtami).
 * Použitie:
 * - Bežný user má prístup k vlastným účtom (access_level = 1)
 * - Family Manager má prístup k účtom detí (access_level = 2)
 * - Admin má prístup ku všetkým (access_level = 3)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAccess {

    private int userId;
    private int accountId;
    private int accessLevel;
    private LocalDateTime grantedAt;

    /**
     * Konvertuje access_level na Role enum.
     */
    public Role toRole() {
        return Role.fromAccessLevel(this.accessLevel);
    }
}
