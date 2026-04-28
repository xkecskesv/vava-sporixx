package sk.sporixx.service;

import sk.sporixx.dto.MilestoneData;

public interface MilestoneService {

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
}