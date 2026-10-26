package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;

public record BillingCommand(String customerId,
                             String planType,
                             Money basePrice,
                             LocalDate signupDate,
                             int overdueDays,
                             String promoCode,
                             boolean nonProfit,
                             String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(customerId, "O cliente é obrigatório");
        Objects.requireNonNull(basePrice, "O preço base é obrigatório");
        Objects.requireNonNull(signupDate, "A data de adesão é obrigatória");
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Os dias de atraso não podem ser negativos");
        }
    }
}
