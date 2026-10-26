package com.saas.billing.infrastructure;

import com.saas.billing.domain.Money;
import com.saas.billing.port.InvoiceRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcInvoiceRepository implements InvoiceRepository {

    private final String url;
    private final String user;
    private final String password;

    public JdbcInvoiceRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public void save(String customerId, Money amount, boolean paid) {
        String sql = "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId);
            stmt.setBigDecimal(2, amount.amount());
            stmt.setString(3, paid ? "PAID" : "FAILED");
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
