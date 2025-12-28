package com.taskmanagement.model.repository;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

// RepositoryTestProfile.java - Тестовый профиль
public class RepositoryTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.datasource.db-kind", "h2",
                "quarkus.datasource.jdbc.url", "jdbc:h2:mem:testdb",
                "quarkus.hibernate-orm.database.generation", "drop-and-create",
                "quarkus.hibernate-orm.log.sql", "true"
        );
    }
}
