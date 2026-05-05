package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO, ktoré uchováva dáta potrebné ku každému family requestu (posiela ho rodič, ktorý si chce pridať dieťa od rodinky).
 * UI zobrazuje pending family request dáta na Overview obrazovke používateľa, ktorý tento request dostal.
 */
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