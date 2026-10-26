package com.saas.billing.port;

import com.saas.billing.domain.Money;

public interface NotificationService {

    void notifyInvoice(String email, Money amount);
}
