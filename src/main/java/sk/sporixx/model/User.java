package sk.sporixx.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre používateľa.
 *
 * Mapuje sa na DB tabuľku 'users'.
 *
 * Lombok anotácie:
 * @Data             - gettery, settery, toString, equals, hashCode
 * @Builder          - User.builder().email("...").build()
 * @NoArgsConstructor - prázdny konštruktor pre JDBC mapovanie
 * @AllArgsConstructor - konštruktor so všetkými poľami
 *
 * Žiadne public polia - enkapsulácia cez Lombok @Data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private int id;
    private String email;
    private String nickname;
    private int roleId;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;
}