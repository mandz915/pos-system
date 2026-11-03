package com.pos.common.tenant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantAwareDataSourceTest {

    private DataSource target;
    private Connection connection;
    private PreparedStatement statement;
    private TenantAwareDataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        target = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(target.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(TenantAwareDataSource.SQL)).thenReturn(statement);
        dataSource = new TenantAwareDataSource(target);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void everyCheckoutTellsTheDatabaseWhichTenantIsCalling() throws SQLException {
        TenantContext.set(42L);

        dataSource.getConnection();

        verify(statement).setString(1, "42");
        verify(statement).setString(2, "off");
        verify(statement).execute();
    }

    @Test
    void withoutATenantTheSettingIsEmptySoNoRowMatches() throws SQLException {
        dataSource.getConnection();

        verify(statement).setString(1, "");
    }

    @Test
    void systemModeIsPassedOn() throws SQLException {
        TenantContext.callAsSystem(() -> {
            try {
                dataSource.getConnection();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return null;
        });

        verify(statement).setString(2, "on");
    }

    @Test
    void aConnectionThatCannotBeScopedIsClosedAndNotHandedOut() throws SQLException {
        when(statement.execute()).thenThrow(new SQLException("boom"));

        try {
            dataSource.getConnection();
        } catch (SQLException expected) {

        }

        verify(connection).close();
        verify(connection, never()).commit();
    }
}
