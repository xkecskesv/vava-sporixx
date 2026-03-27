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
    private String name;
    private LocalDateTime createdAt;
    private Role role;

    public boolean hasRole(Role expectedRole) {
        return this.role == expectedRole;
    }
}