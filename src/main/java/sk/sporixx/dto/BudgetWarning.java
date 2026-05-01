package sk.sporixx.dto;

public enum BudgetWarning {
    /** Žiadne upozornenie */
    NONE,
    /** Pri Custom Allocation - essential < skutočné výdavky z Budget Setup */
    ESSENTIAL_BELOW_ACTUAL,
    /** Pri Budget Setup - essential príliš vysoké, použitý fallback */
    FALLBACK_ALLOCATION_APPLIED
}
