package sk.sporixx.model;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Dátový model pre family requests.
 * Mapuje sa na DB tabuľku 'family_requests'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyRequest {

    private int id;
    private int fromUserId;
    private int toUserId;
    private String status; // PENDING, ACCEPTED, REJECTED
    private LocalDateTime createdAt;

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
}
