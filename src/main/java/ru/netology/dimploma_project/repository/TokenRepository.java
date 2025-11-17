package ru.netology.dimploma_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.dimploma_project.model.Token;
import ru.netology.dimploma_project.model.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long>{
    Optional<Token> findByToken(String token);

    List<Token> findAllByUser(User user);

    void deleteAllByExpiresAtBefore(Instant time);

    void deleteAllByUser(User user);
}
