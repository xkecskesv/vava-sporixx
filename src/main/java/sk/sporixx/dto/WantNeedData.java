package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO pre Want vs Need stĺpcový graf.
 */
@Data
@Builder
public class WantNeedData {
    private double totalWant;
    private double totalNeed;

    /** Pre percento v UI */
    private double wantPercentage;
    private double needPercentage;
}
