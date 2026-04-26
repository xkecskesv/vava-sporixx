package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import sk.sporixx.model.Role;

/**
 * DTO pre prihláseného používateľa — bez passwordHash.
 * Toto vidí UI vrstva. Service vrstva pracuje s plným User modelom.
 */
@Data
@Builder
public class CurrentUser {
    private int id;
    private String name;
    private String surname;
    private String email;
    private String gender;
    private String photoPath;  // môže byť null — nie každý má fotku
    private Role role;

    /** Helper pre zobrazenie v UI (sidebar, profile header) */
    public String getFullName() {
        return name + " " + surname;
    }

    // UI použije pre checkbox "I am parent"
    public boolean checkisParent() {
        return role == Role.FAMILY_MANAGER;
    }

    public boolean checkhasPhoto() {
        return photoPath != null && !photoPath.isBlank();
    }
}
