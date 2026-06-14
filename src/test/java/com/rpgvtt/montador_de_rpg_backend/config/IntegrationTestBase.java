package com.rpgvtt.montador_de_rpg_backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for all integration tests.
 *
 * Uses a real PostgreSQL container so JSONB operators, JSONB storage,
 * and Hypersistence Utils mappings all behave identically to production.
 *
 * Each test runs in a transaction that is rolled back after the test — no
 * manual cleanup needed.
 */
@SpringBootTest
@Testcontainers
@Transactional
public abstract class IntegrationTestBase {

    // Shared container — started once for the entire test run, not per test
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("db_rpg_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Use Liquibase/Flyway to create schema — same scripts as production
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    protected EntityManager em;

    /**
     * Forces JPA to flush writes to the DB and clears the first-level cache,
     * so subsequent reads hit the DB rather than returning cached entities.
     *
     * Call this between the "act" and "assert" phases when testing that a
     * service method actually persisted something.
     */
    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}