package com.xinyue.atelier.service;

import com.xinyue.atelier.model.Pattern;
import com.xinyue.atelier.respository.PatternRepo;
import org.springframework.stereotype.Service;

@Service
public class PatternService {
    private final PatternRepo repo;

    public PatternService(PatternRepo repo) {this.repo = repo;}
    public Pattern create(Pattern pattern) {
       return repo.save(pattern);
    }
}
