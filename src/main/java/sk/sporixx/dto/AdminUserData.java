package sk.sporixx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Row DTO for the admin users table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserData {
    private int id;
    private String name;
    private String email;
    private boolean familyManager;
    private boolean active;
}

