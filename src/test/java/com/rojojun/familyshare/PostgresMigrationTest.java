package com.rojojun.familyshare;

import com.rojojun.familyshare.user.AppUserModel;
import com.rojojun.familyshare.user.AppUserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Use an empty, disposable PostgreSQL database, never the production database. */
@EnabledIfEnvironmentVariable(named = "POSTGRES_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${POSTGRES_TEST_URL}",
        "spring.datasource.username=${POSTGRES_TEST_USER:postgres}",
        "spring.datasource.password=${POSTGRES_TEST_PASSWORD:postgres}",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PostgresMigrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AppUserRepository users;
    @Autowired EntityManager entityManager;

    @Test
    @Transactional
    void flywayCreatesSchemaThatHibernateCanReadAndWrite() {
        assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class))
                .isEqualTo(1);

        var user = users.saveAndFlush(AppUserModel.of("postgres-migration-test-user"));
        var id = user.getId();
        entityManager.clear();

        var reloaded = users.findById(id).orElseThrow();
        assertThat(reloaded.getAnonKey()).isEqualTo("postgres-migration-test-user");
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
}
