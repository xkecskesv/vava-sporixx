package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MilestoneData {
    private String category;      // "Smart Spender", "Saving Master" atď.
    private int level;            // 0-5
    private String levelName;     // "Impulse Buyer", "Careful Spender" atď.
    private double xp;            // level * 10
    private double progress;      // 0.0 - 1.0 pre progress bar
    private String description;   // popis čo treba spraviť pre ďalší level
}