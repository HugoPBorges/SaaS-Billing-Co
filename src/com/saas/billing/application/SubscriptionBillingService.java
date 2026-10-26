package com.saas.billing.application;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PricingCalculator;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;
    private final PricingCalculator pricingCalculator = new PricingCalculator();

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
        Money price = pricingCalculator.calculateMonthlyPrice(command, LocalDate.now(clock));
        try {
            customerRepository.findById(command.customerId())
                    .ifPresent(customer -> processCustomerBilling(command, customer, price, logs));
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }
        return logs;
    }

    private void processCustomerBilling(BillingCommand command, Customer customer, Money price,
                                        List<String> logs) {
        if (!customer.isActive()) {
            logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
            return;
        }

        boolean paymentSuccess = paymentGateway.charge(command.paymentCardToken(), price);
        logs.add(paymentSuccess
                ? "Pagamento processado com sucesso: R$ " + price
                : "Falha no pagamento do cartão para: " + command.customerId());
        invoiceRepository.save(command.customerId(), price, paymentSuccess);
        notificationService.notifyInvoice(customer.email(), price);
        logs.add("E-mail disparado para: " + customer.email());
    }
}
