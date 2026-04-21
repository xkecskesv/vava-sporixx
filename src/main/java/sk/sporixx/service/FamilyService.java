package sk.sporixx.service;

import sk.sporixx.dto.FamilyMemberData;
import sk.sporixx.model.FamilyRequest;

import java.util.List;

public interface FamilyService {

    /**
     * Načíta všetkých členov rodiny (deti) prihláseného Family Managera.
     * Každý člen obsahuje profil + jeho účty.
     */
    List<FamilyMemberData> getFamilyMembers();

    /**
     * Odstráni dieťa z rodiny.
     * Zmaže všetky account_access záznamy pre daného používateľa.
     */
    void removeFamilyMember(int userId);

    /**
     * Pošle požiadavku na pridanie dieťaťa do rodiny.
     * Neudelí prístup hneď — čaká na potvrdenie dieťaťa.
     */
    void sendFamilyRequest(String email);

    /**
     * Načíta všetky pending požiadavky pre prihláseného používateľa.
     * Volá sa pri prihlásení.
     */
    List<FamilyRequest> getPendingRequests();

    /**
     * Dieťa prijme požiadavku — udelí sa prístup rodičovi.
     */
    void acceptFamilyRequest(int requestId);

    /**
     * Dieťa odmietne požiadavku.
     */
    void rejectFamilyRequest(int requestId);

    List<FamilyRequest> getSentRequests();
}