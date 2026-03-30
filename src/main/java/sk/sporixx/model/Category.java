package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre kategóriu transakcie.
 * * Kategória môže byť:
 *  * - systémová (userId = null) - default kategórie pre všetkých
 *  * - používateľská (userId = konkrétny user) - vlastné kategórie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    private int id;
    private Integer userId;
    private String name;
    private LocalDateTime createdAt;

    /**
     * @return true ak je kategória systémová (bez vlastníka)
     */
    public boolean isSystemCategory() { return this.userId == null; }
}