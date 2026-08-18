package com.enterprise.spendsync.budget.internal.domain;

/**
 * Periodicity / Granularity of a Budget Pool.
 */
public enum BudgetPeriodType {
    /**
     * Single budget pool covering the entire fiscal year.
     */
    ANNUAL,

    /**
     * Quarter 1 (Months 1-3).
     */
    Q1,

    /**
     * Quarter 2 (Months 4-6).
     */
    Q2,

    /**
     * Quarter 3 (Months 7-9).
     */
    Q3,

    /**
     * Quarter 4 (Months 10-12).
     */
    Q4,

    /**
     * Monthly budget granularity.
     */
    MONTHLY
}
