package sk.sporixx.service;

import sk.sporixx.dto.BudgetData;
import sk.sporixx.dto.BudgetWarning;

/**
 * Service pre správu rozpočtu používateľa.
 * Pokrýva: Budget Setup, Recommended/Custom Allocation, Emergency Fund.
 */
public interface BudgetService {

    /**
     * Načíta všetky budget dáta pre prihláseného používateľa.
     * Ak nemá budget nastavený, vráti default hodnoty (všetko 0).
     */
    BudgetData loadBudgetData();

    /**
     * Uloží Budget Setup a prepočíta Recommended Allocation.
     * @return BudgetWarning.FALLBACK_ALLOCATION_APPLIED ak bol použitý fallback
     */
    BudgetWarning saveBudgetSetup(double monthlyIncome, double food, double rent, double transport, double utilities, double other);

    /**
     * Uloží Custom Allocation (používateľ zadá vlastné sumy).
     * @return BudgetWarning.ESSENTIAL_BELOW_ACTUAL ak essential < skutočné výdavky
     */
    BudgetWarning saveCustomAllocation(double essentialExpenses, double emergencyFund, double savings, double toInvest);
}