package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre rozpočtovú šablónu.
 * Predpripravené šablóny:
 *   "50/30/20"           -> needs=0.50, wants=0.30, saving=0.20
 *   "Pay Yourself First" -> needs=0.50, wants=0.30, saving=0.20 (saving first)
 *   "Zero-Based"         -> needs=0.00, wants=0.00, saving=0.00 (všetko manuálne)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTemplate {

    private int id;
    private String name;
    private Integer userId;
    private double needs;     // 0.50 = 50%
    private double wants;     // 0.30 = 30%
    private double saving;    // 0.20 = 20%
    private String description;
    private LocalDateTime createdAt;
}