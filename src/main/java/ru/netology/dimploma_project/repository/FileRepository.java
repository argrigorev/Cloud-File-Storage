package ru.netology.dimploma_project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.dimploma_project.model.FileEntity;
import ru.netology.dimploma_project.model.User;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findAllByOwner(User owner);

    Optional<FileEntity> findByOwnerAndFilename(User owner, String filename);

    void deleteByOwnerAndFilename(User owner, String filename);
}
