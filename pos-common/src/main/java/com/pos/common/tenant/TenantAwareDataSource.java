package com.pos.common.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

public class TenantAwareDataSource extends DelegatingDataSource {

    static final String SQL = "SELECT set_config('app.tenant_id', ?, false), set_config('app.system', ?, false)";

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return scoped(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return scoped(super.getConnection(username, password));
    }

    private static Connection scoped(Connection connection) throws SQLException {
        try {
            Long tenant = TenantContext.currentOrNull();
            try (PreparedStatement statement = connection.prepareStatement(SQL)) {
                statement.setString(1, tenant == null ? "" : String.valueOf(tenant));
                statement.setString(2, TenantContext.isSystem() ? "on" : "off");
                statement.execute();
            }
            return connection;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.close();
            } catch (SQLException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }
}
