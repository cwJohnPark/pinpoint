package com.navercorp.pinpoint.testapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class DataSourceStressService {

    private Logger logger = LoggerFactory.getLogger(DataSourceStressService.class);

    private final DataSource smallSizeDataSource;

    public DataSourceStressService(DataSource smallSizeDataSource) {
        this.smallSizeDataSource = smallSizeDataSource;
    }

    public boolean occupyDataSource() {

        boolean executed = true;

        try(Connection conn = smallSizeDataSource.getConnection();
            Connection conn2 = smallSizeDataSource.getConnection()
        ) {
            Statement stmt = conn.createStatement();
            executed &= stmt.execute("SELECT 1");

            stmt = conn2.createStatement();
            executed &= stmt.execute("SELECT 2");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return executed;
    }
}
