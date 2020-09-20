package com.navercorp.pinpoint.testapp.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean
    public DataSource smallSizeDataSource() {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl("jdbc:h2:mem:test");
        configuration.setMaximumPoolSize(1);
        configuration.setConnectionTimeout(500L);
        return new HikariDataSource(configuration);
    }

}
