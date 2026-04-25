package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;
import sk.sporixx.model.Account;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FamilyMemberData {
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private String photoPath;
    private LocalDateTime grantedAt;
    private List<Account> accounts;
}
