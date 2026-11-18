package com.dogus.vacation.domain.policy;

/**
 * {@link WeekdayWorkingDayCalculator}'ı ortak sözleşme testlerinden geçirir.
 * Spring gerekmez: saf birim testi, milisaniyeler içinde çalışır.
 */
class WeekdayWorkingDayCalculatorTest extends WorkingDayCalculatorContractTest {

    @Override
    protected WorkingDayCalculator calculator() {
        return new WeekdayWorkingDayCalculator();
    }
}
