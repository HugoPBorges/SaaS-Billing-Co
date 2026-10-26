package com.saas.billing.application;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Money;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.mail.SimpleEmail;

public class SubscriptionBillingService {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/saas_db";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin123";

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        LocalDate today = LocalDate.now();

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

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sqlCustomer = "SELECT email, status FROM customers WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sqlCustomer);
            stmt.setString(1, command.customerId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                String status = rs.getString("status");

                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                    return logs;
                }

                boolean paymentSuccess = false;
                if (command.paymentCardToken() != null && command.paymentCardToken().startsWith("tok_")) {
                    paymentSuccess = true;
                    logs.add("Pagamento processado com sucesso: R$ " + calculatedPrice);
                } else {
                    logs.add("Falha no pagamento do cartão para: " + command.customerId());
                }

                String sqlInvoice = "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";
                PreparedStatement stmtInvoice = conn.prepareStatement(sqlInvoice);
                stmtInvoice.setString(1, command.customerId());
                stmtInvoice.setBigDecimal(2, calculatedPrice.amount());
                stmtInvoice.setString(3, paymentSuccess ? "PAID" : "FAILED");
                stmtInvoice.executeUpdate();

                SimpleEmail emailSender = new SimpleEmail();
                emailSender.setHostName("smtp.empresa.com");
                emailSender.setSmtpPort(587);
                emailSender.setFrom("billing@empresa.com");
                emailSender.setSubject("Sua Fatura Mensal");
                emailSender.setMsg("Olá! Sua fatura foi processada no valor de R$ " + calculatedPrice);
                emailSender.addTo(email);

                logs.add("E-mail disparado para: " + email);
            }
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}
