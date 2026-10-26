package com.saas.billing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Money;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SubscriptionBillingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void orchestratesBillingWithCalculatedPrice() {
        Money expectedPrice = Money.of("85.00");
        AtomicReference<Money> chargedAmount = new AtomicReference<>();
        AtomicReference<Money> invoicedAmount = new AtomicReference<>();
        AtomicReference<Money> notifiedAmount = new AtomicReference<>();

        SubscriptionBillingService service = service(
                id -> Optional.of(new Customer(id, "client@example.com", "ACTIVE")),
                (token, amount) -> {
                    chargedAmount.set(amount);
                    return true;
                },
                (id, amount, paid) -> invoicedAmount.set(amount),
                (email, amount) -> notifiedAmount.set(amount));

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(expectedPrice, chargedAmount.get());
        assertEquals(expectedPrice, invoicedAmount.get());
        assertEquals(expectedPrice, notifiedAmount.get());
        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 85.00",
                "E-mail disparado para: client@example.com"), logs);
    }

    @Test
    void stopsBeforeExternalEffectsWhenCustomerIsInactive() {
        AtomicBoolean paymentCalled = new AtomicBoolean();
        AtomicBoolean invoiceCalled = new AtomicBoolean();
        AtomicBoolean notificationCalled = new AtomicBoolean();

        SubscriptionBillingService service = service(
                id -> Optional.of(new Customer(id, "client@example.com", "INACTIVE")),
                (token, amount) -> {
                    paymentCalled.set(true);
                    return true;
                },
                (id, amount, paid) -> invoiceCalled.set(true),
                (email, amount) -> notificationCalled.set(true));

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: customer-1"), logs);
        assertFalse(paymentCalled.get());
        assertFalse(invoiceCalled.get());
        assertFalse(notificationCalled.get());
    }

    @Test
    void keepsPaymentLogWhenAFollowingOperationFails() {
        SubscriptionBillingService service = service(
                id -> Optional.of(new Customer(id, "client@example.com", "ACTIVE")),
                (token, amount) -> true,
                (id, amount, paid) -> {
                    throw new IllegalStateException("database unavailable");
                },
                (email, amount) -> { });

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(List.of(
                "Pagamento processado com sucesso: R$ 85.00",
                "Erro fatal no processamento: database unavailable"), logs);
    }

    @Test
    void doesNothingWhenCustomerDoesNotExist() {
        AtomicBoolean paymentCalled = new AtomicBoolean();
        SubscriptionBillingService service = service(
                id -> Optional.empty(),
                (token, amount) -> {
                    paymentCalled.set(true);
                    return true;
                },
                (id, amount, paid) -> { },
                (email, amount) -> { });

        assertTrue(service.processMonthlyBilling(command()).isEmpty());
        assertFalse(paymentCalled.get());
    }

    private SubscriptionBillingService service(
            CustomerRepository customerRepository,
            PaymentGateway paymentGateway,
            InvoiceRepository invoiceRepository,
            NotificationService notificationService) {
        return new SubscriptionBillingService(customerRepository, invoiceRepository,
                paymentGateway, notificationService, CLOCK);
    }

    private BillingCommand command() {
        return new BillingCommand("customer-1", "STANDARD", Money.of("100.00"),
                LocalDate.of(2024, 10, 1), 0, null, false, "tok_valid");
    }
}
