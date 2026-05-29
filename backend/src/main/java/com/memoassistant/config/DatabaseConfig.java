package com.memoassistant.config;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class DatabaseConfig {
    @Bean
    DataSource dataSource(@Value("${spring.datasource.url}") String url,
                          @Value("${spring.datasource.driver-class-name}") String driverClassName) throws Exception {
        createSqliteParentDirectory(url);
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    @Bean
    JdbcClient jdbcClient(JdbcTemplate jdbcTemplate) {
        return JdbcClient.create(jdbcTemplate);
    }

    private void createSqliteParentDirectory(String url) throws Exception {
        if (!url.startsWith("jdbc:sqlite:")) {
            return;
        }
        String dbPath = url.substring("jdbc:sqlite:".length());
        if (dbPath.isBlank() || ":memory:".equals(dbPath)) {
            return;
        }
        Path parent = Path.of(dbPath).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
