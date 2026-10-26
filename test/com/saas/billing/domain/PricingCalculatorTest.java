package com.saas.billing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private static final LocalDate BILLING_DATE = LocalDate.of(2026, 11, 15);
    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void respectsLoyaltyDiscountBoundaries() {
        assertEquals(Money.of("100.00"), calculator.calculateMonthlyPrice(command("100.00",
                LocalDate.of(2025, 11, 1), 0, null, false), BILLING_DATE));
        assertEquals(Money.of("90.00"), calculator.calculateMonthlyPrice(command("100.00",
                LocalDate.of(2024, 11, 1), 0, null, false), BILLING_DATE));
    }

    @Test
    void appliesLoyaltyDiscountsAfterTwelveAndTwentyFourMonths() {
        assertEquals(Money.of("90.00"), calculator.calculateMonthlyPrice(command("100.00",
                LocalDate.of(2025, 10, 1), 0, null, false), BILLING_DATE));
        assertEquals(Money.of("85.00"), calculator.calculateMonthlyPrice(command("100.00",
                LocalDate.of(2024, 10, 1), 0, null, false), BILLING_DATE));
    }

    @Test
    void appliesNonProfitAndBlackFridayDiscountsBeforeLateCharges() {
        BillingCommand command = command("100.00", LocalDate.of(2024, 10, 1),
                10, "BLACKFRIDAY", true);

        assertEquals(Money.of("30.39"), calculator.calculateMonthlyPrice(command, BILLING_DATE));
    }

    @Test
    void appliesWelcomePromotionOnlyDuringFirstMonth() {
        BillingCommand eligible = command("100.00", LocalDate.of(2026, 10, 1),
                0, "welcome10", false);
        BillingCommand ineligible = command("100.00", LocalDate.of(2026, 9, 1),
                0, "WELCOME10", false);

        assertEquals(Money.of("90.00"), calculator.calculateMonthlyPrice(eligible, BILLING_DATE));
        assertEquals(Money.of("100.00"), calculator.calculateMonthlyPrice(ineligible, BILLING_DATE));
    }

    @Test
    void ignoresBlackFridayPromotionOutsideNovember() {
        BillingCommand command = command("100.00", LocalDate.of(2026, 6, 1),
                0, "BLACKFRIDAY", false);

        assertEquals(Money.of("100.00"), calculator.calculateMonthlyPrice(command,
                LocalDate.of(2026, 7, 15)));
    }

    @Test
    void neverReturnsNegativePrice() {
        BillingCommand command = command("20.00", LocalDate.of(2026, 11, 1),
                0, "BLACKFRIDAY", false);

        assertEquals(Money.ZERO, calculator.calculateMonthlyPrice(command, BILLING_DATE));
    }

    private BillingCommand command(String basePrice, LocalDate signupDate, int overdueDays,
                                   String promoCode, boolean nonProfit) {
        return new BillingCommand("customer-1", "STANDARD", Money.of(basePrice), signupDate,
                overdueDays, promoCode, nonProfit, "tok_valid");
    }
}
