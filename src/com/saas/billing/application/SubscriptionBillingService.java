package com.saas.billing.application;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Money;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      InvoiceRepository invoiceRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentGateway = paymentGateway;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - command.signupDate().getYear()) * 12 + (today.getMonthValue() - command.signupDate().getMonthValue());

        Money base = command.basePrice();
        Money calculatedPrice = base;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice.subtract(base.multiply(new BigDecimal("0.15")));
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice.subtract(base.multiply(new BigDecimal("0.10")));
        }

        if (command.nonProfit()) {
            calculatedPrice = calculatedPrice.multiply(new BigDecimal("0.70"));
        }

        if (command.promoCode() != null && !command.promoCode().trim().isEmpty()) {
            if (command.promoCode().equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice.subtract(Money.of("30.00"));
            } else if (command.promoCode().equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice.multiply(new BigDecimal("0.90"));
            }
        }

        if (command.overdueDays() > 0) {
            Money penalty = calculatedPrice.multiply(new BigDecimal("0.02"));
            Money dailyInterest = calculatedPrice.multiply(new BigDecimal("0.001").multiply(BigDecimal.valueOf(command.overdueDays())));
            calculatedPrice = calculatedPrice.add(penalty).add(dailyInterest);
        }

        calculatedPrice = calculatedPrice.max(Money.ZERO);

        try {
            Optional<Customer> found = customerRepository.findById(command.customerId());

            if (found.isPresent()) {
                Customer customer = found.get();

                if (!customer.isActive()) {
                    logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                    return logs;
                }

                boolean paymentSuccess = paymentGateway.charge(command.paymentCardToken(), calculatedPrice);
                if (paymentSuccess) {
                    logs.add("Pagamento processado com sucesso: R$ " + calculatedPrice);
                } else {
                    logs.add("Falha no pagamento do cartão para: " + command.customerId());
                }

                invoiceRepository.save(command.customerId(), calculatedPrice, paymentSuccess);

                notificationService.notifyInvoice(customer.email(), calculatedPrice);

                logs.add("E-mail disparado para: " + customer.email());
            }
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}
