package com.xinyue.atelier.repository;

import com.xinyue.atelier.model.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatternRepo extends JpaRepository<Pattern, UUID> {
    List<Pattern> findAllByFolderId(UUID folderId);
}
