package sk.sporixx.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre používateľa.
 * Mapuje sa na DB tabuľku 'users'.
 * Žiadne public polia - enkapsulácia cez Lombok @Data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private int id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String photoPath;   // nullable
    private String gender;
    private LocalDateTime createdAt;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private Role role = Role.USER;

    public boolean hasRole(Role expectedRole) {
        return this.role == expectedRole;
    }
}