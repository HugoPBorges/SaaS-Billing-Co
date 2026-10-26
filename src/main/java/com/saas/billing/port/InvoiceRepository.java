package com.saas.billing.port;

import com.saas.billing.domain.Money;

public interface InvoiceRepository {

    void save(String customerId, Money amount, boolean paid);
}
