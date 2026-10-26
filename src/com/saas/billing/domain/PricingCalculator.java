package com.saas.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class PricingCalculator {

    private static final BigDecimal LONG_TERM_DISCOUNT_RATE = new BigDecimal("0.15");
    private static final BigDecimal LOYALTY_DISCOUNT_RATE = new BigDecimal("0.10");
    private static final BigDecimal NON_PROFIT_PRICE_FACTOR = new BigDecimal("0.70");
    private static final BigDecimal WELCOME_PRICE_FACTOR = new BigDecimal("0.90");
    private static final BigDecimal LATE_PAYMENT_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");

    public Money calculateMonthlyPrice(BillingCommand command, LocalDate billingDate) {
        Objects.requireNonNull(command, "O comando de cobrança é obrigatório");
        Objects.requireNonNull(billingDate, "A data de cobrança é obrigatória");

        int monthsActive = monthsActive(command.signupDate(), billingDate);
        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromotion(price, command.promoCode(), monthsActive, billingDate);
        price = applyLateCharges(price, command.overdueDays());
        return price.max(Money.ZERO);
    }

    private int monthsActive(LocalDate signupDate, LocalDate billingDate) {
        return (billingDate.getYear() - signupDate.getYear()) * 12
                + billingDate.getMonthValue() - signupDate.getMonthValue();
    }

    private Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > 24) {
            return basePrice.subtract(basePrice.multiply(LONG_TERM_DISCOUNT_RATE));
        }
        if (monthsActive > 12) {
            return basePrice.subtract(basePrice.multiply(LOYALTY_DISCOUNT_RATE));
        }
        return basePrice;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.multiply(NON_PROFIT_PRICE_FACTOR) : price;
    }

    private Money applyPromotion(Money price, String promoCode, int monthsActive,
                                 LocalDate billingDate) {
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return price;
        }
        if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && billingDate.getMonthValue() == 11) {
            return price.subtract(BLACK_FRIDAY_DISCOUNT);
        }
        if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
            return price.multiply(WELCOME_PRICE_FACTOR);
        }
        return price;
    }

    private Money applyLateCharges(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.multiply(LATE_PAYMENT_PENALTY_RATE);
        Money interest = price.multiply(DAILY_INTEREST_RATE.multiply(BigDecimal.valueOf(overdueDays)));
        return price.add(penalty).add(interest);
    }
}
