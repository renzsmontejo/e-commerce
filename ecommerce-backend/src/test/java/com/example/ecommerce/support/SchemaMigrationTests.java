package com.example.ecommerce.support;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import static org.junit.jupiter.api.Assertions.*;

class SchemaMigrationTests extends MySqlIntegrationTest {
    @Test void upgradesUnversionedLegacySchemaWithoutLosingData() throws Exception {
        String schema="migration_test_"+UUID.randomUUID().toString().replace("-","");
        String rootUrl=MYSQL.getJdbcUrl();
        try(var root=DriverManager.getConnection(rootUrl,"root",MYSQL.getPassword());var statement=root.createStatement()) {
            statement.execute("CREATE DATABASE `"+schema+"`");
            String url=rootUrl.replace("/ecommerce_test","/"+schema);
            try(var connection=DriverManager.getConnection(url,"root",MYSQL.getPassword());var sql=connection.createStatement()) {
                ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/migration/V1__initial_schema.sql"));
                sql.execute("ALTER TABLE users ADD COLUMN password VARCHAR(200) NOT NULL DEFAULT 'legacy-value'");
                sql.execute("ALTER TABLE products ADD COLUMN brand VARCHAR(100) NOT NULL DEFAULT 'Legacy Brand', ADD COLUMN is_featured BIT NOT NULL DEFAULT 0, MODIFY COLUMN description TEXT NOT NULL");
                sql.execute("INSERT INTO users (first_name,last_name,email,password_hash,status,created_at,updated_at) VALUES ('Old','Customer','legacy@example.invalid','preserved-hash','ACTIVE',NOW(),NOW())");
                sql.execute("INSERT INTO categories (name,slug,is_active,created_at,updated_at) VALUES ('Legacy','legacy',1,NOW(),NOW())");
                sql.execute("INSERT INTO products (name,slug,description,status,category_id,created_at,updated_at) SELECT 'Existing Product','existing-product','','ACTIVE',id,NOW(),NOW() FROM categories WHERE slug='legacy'");
                sql.execute("ALTER TABLE products ALTER COLUMN brand DROP DEFAULT, ALTER COLUMN is_featured DROP DEFAULT");
                var unsafe=Flyway.configure().dataSource(url,"root",MYSQL.getPassword()).load();
                assertThrows(FlywayException.class,unsafe::migrate);
                var flyway=Flyway.configure().dataSource(url,"root",MYSQL.getPassword()).baselineOnMigrate(true).baselineVersion("0").load();
                flyway.migrate();flyway.validate();
                try(var rows=sql.executeQuery("SELECT password_hash,password FROM users WHERE email='legacy@example.invalid'")) {
                    assertTrue(rows.next());assertEquals("preserved-hash",rows.getString(1));assertEquals("legacy-value",rows.getString(2));
                }
                try(var rows=sql.executeQuery("SELECT brand FROM products WHERE slug='existing-product'")) {assertTrue(rows.next());assertEquals("Legacy Brand",rows.getString(1));}
                sql.execute("INSERT INTO users (first_name,last_name,email,password_hash,status,created_at,updated_at) VALUES ('New','Customer','new@example.invalid','new-hash','ACTIVE',NOW(),NOW())");
                sql.execute("INSERT INTO products (name,slug,status,category_id,created_at,updated_at) SELECT 'New Product','new-product','ACTIVE',id,NOW(),NOW() FROM categories WHERE slug='legacy'");
                try(var rows=sql.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1 AND version IN ('1','2','3')")){rows.next();assertEquals(3,rows.getInt(1));}
                try(var rows=sql.executeQuery("SELECT COUNT(*) FROM roles WHERE name IN ('CUSTOMER','ADMIN')")){rows.next();assertEquals(2,rows.getInt(1));}
            } finally {
                if(!schema.matches("migration_test_[a-f0-9]{32}"))throw new IllegalStateException("Unexpected test schema");
                statement.execute("DROP DATABASE `"+schema+"`");
            }
        }
    }
}
