package sk.sporixx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO reprezentujúce jeden riadok tabuľky používateľov v administrátorskom paneli.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserData {
    private int id;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private String gender;
    private String photoPath;
    private boolean admin;
    private boolean familyManager;
    private boolean active;
}

