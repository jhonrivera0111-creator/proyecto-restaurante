package com.casaverde.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
@EnableCaching
public class DataSourceConfig {

    @Value("${app.datasource.path:./DataBase/casaverde.db}")
    private String dbPath;

    @Bean
    DataSource dataSource() {
        try {
            Path path = Paths.get(dbPath);
            Path dir = path.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de base de datos para: " + dbPath, e);
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.sqlite.JDBC");
        ds.setUrl("jdbc:sqlite:" + dbPath + "?foreign_keys=on");
        return ds;
    }
}
