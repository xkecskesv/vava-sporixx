package sk.sporixx.repository;

import sk.sporixx.model.FamilyRequest;
import java.util.List;
import java.util.Optional;

public interface FamilyRequestRepository {

    FamilyRequest save(FamilyRequest request);

    Optional<FamilyRequest> findById(int id);

    /**
     * Načíta všetky pending požiadavky pre dané dieťa.
     * Volá sa pri prihlásení dieťaťa.
     */
    List<FamilyRequest> findPendingByToUserId(int toUserId);

    /**
     * Skontroluje či už existuje pending požiadavka medzi rodičom a dieťaťom.
     */
    boolean existsPending(int fromUserId, int toUserId);

    void updateStatus(int requestId, String status);

    List<FamilyRequest> findPendingByFromUserId(int fromUserId);
}
