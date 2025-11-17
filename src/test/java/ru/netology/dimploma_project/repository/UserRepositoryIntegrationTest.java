package ru.netology.dimploma_project.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.netology.dimploma_project.model.User;

@SpringBootTest
@Testcontainers
public class UserRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("testdb")
                    .withUsername("postgres")
                    .withPassword("admin");

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void testSaveAndFindUser() {
        User user = new User();
        user.setUsername("artem");
        user.setPassword("123");

        userRepository.save(user);

        User found = userRepository.findByUsername("artem")
                .orElseThrow(() -> new RuntimeException("User not found"));

        Assertions.assertNotNull(found);
        Assertions.assertEquals("artem", found.getUsername());
    }
}
