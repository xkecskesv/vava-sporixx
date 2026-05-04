package sk.sporixx.service;

import sk.sporixx.dto.MilestoneData;
import sk.sporixx.model.User;

import java.util.List;

public interface MilestoneService {

    /**
     * Vráti uložené milestones pre daného používateľa bez prepočítavania.
     * Používa sa pre zobrazenie milestones členov rodiny.
     */
    List<MilestoneData> getMilestonesFromUser(User user);

    /**
     * Vypočíta Smart Spender level podľa pomeru Want/Need výdavkov.
     * Počíta sa za posledných 12 mesiacov.
     * Nevyžaduje ukladanie do DB - vypočíta sa dynamicky.
     */
    MilestoneData getSmartSpenderMilestone();

    /**
     * Vypočíta Saving Master milestone podľa výšky úspor.
     */
    MilestoneData getSavingMasterMilestone();

    /**
     * Vypočíta Investor milestone podľa investičných transakcií.
     */
    MilestoneData getInvestorMilestone();

    /**
     * Vypočíta Budget Keeper milestone podľa dodržiavania budgetu.
     */
    MilestoneData getBudgetKeeperMilestone();

    /**
     * Vráti lokalizačný kľúč titulu používateľa podľa celkového XP (0-200).
     * 0–39 = started, 40–79 = aware, 80–119 = skilled, 120–159 = expert, 160–200 = pro
     */
    String getFinancialTitleKey(int totalXp);
}