package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class FamilyRequestData {
    private int requestId;
    private int fromUserId;
    private String fromFirstName;
    private String fromLastName;
    private String fromEmail;
    private LocalDateTime createdAt;
}