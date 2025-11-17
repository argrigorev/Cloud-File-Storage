package ru.netology.dimploma_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.dimploma_project.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
