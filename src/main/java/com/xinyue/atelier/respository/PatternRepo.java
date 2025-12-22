package com.xinyue.atelier.respository;

import com.xinyue.atelier.model.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatternRepo extends JpaRepository<Pattern, Long> {
}
