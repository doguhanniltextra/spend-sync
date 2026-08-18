package com.enterprise.spendsync.budget.internal.domain;

/**
 * Policy governing budget overrun behavior when reservations exceed available funds.
 */
public enum BudgetEnforcementMode {
    /**
     * Strict hard stop. Any PR reservation exceeding available funds is immediately blocked.
     */
    HARD_STOP,

    /**
     * Warning / Soft stop. Overrun is flagged and accepted, triggering mandatory executive escalation.
     */
    SOFT_STOP,

    /**
     * Tolerance window. Overrun up to a configured percentage (e.g. 5%) is permitted automatically;
     * amounts beyond tolerance are hard stopped.
     */
    TOLERANCE
}
