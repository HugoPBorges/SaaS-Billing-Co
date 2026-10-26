package com.saas.billing.infrastructure;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationService;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class EmailNotificationService implements NotificationService {

    @Override
    public void notifyInvoice(String email, Money amount) {
        try {
            SimpleEmail emailSender = new SimpleEmail();
            emailSender.setHostName("smtp.empresa.com");
            emailSender.setSmtpPort(587);
            emailSender.setFrom("billing@empresa.com");
            emailSender.setSubject("Sua Fatura Mensal");
            emailSender.setMsg("Olá! Sua fatura foi processada no valor de R$ " + amount);
            emailSender.addTo(email);
        } catch (EmailException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
