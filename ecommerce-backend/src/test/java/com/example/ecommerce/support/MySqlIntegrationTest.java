package com.example.ecommerce.support;

import org.testcontainers.mysql.MySQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** One disposable MySQL per test JVM; Ryuk removes it when the test JVM exits. */
public abstract class MySqlIntegrationTest {
    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46")
            .withDatabaseName("ecommerce_test").withUsername("test").withPassword("test");
    static { MYSQL.start(); }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username",MYSQL::getUsername);
        properties.add("spring.datasource.password",MYSQL::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto",()->"validate");
        properties.add("spring.jpa.show-sql",()->"false");
        properties.add("app.jwt.secret",()->"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }
}
