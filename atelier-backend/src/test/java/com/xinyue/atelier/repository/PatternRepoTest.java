package com.xinyue.atelier.repository;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PatternRepoTest {

    @Autowired
    private PatternRepo patternRepo;

    @Autowired
    private FolderRepo folderRepo;

    // ---------- findAllByFolderId ----------

    @Test
    void findAllByFolderId_returnsPatternsBelongingToFolder() {
        Folder folder = newFolder("Folder One");
        folderRepo.save(folder);

        Pattern pattern1 = newPattern("Pattern One", folder);
        Pattern pattern2 = newPattern("Pattern Two", folder);
        patternRepo.save(pattern1);
        patternRepo.save(pattern2);

        List<Pattern> result = patternRepo.findAllByFolderId(folder.getId());

        assertThat(result)
                .extracting(Pattern::getTitle)
                .containsExactlyInAnyOrder("Pattern One", "Pattern Two");
    }

    @Test
    void findAllByFolderId_excludesPatternsFromOtherFolders() {
        Folder folder1 = newFolder("Folder One");
        Folder folder2 = newFolder("Folder Two");
        folderRepo.save(folder1);
        folderRepo.save(folder2);

        Pattern patternInFolder1 = newPattern("Pattern In Folder One", folder1);
        Pattern patternInFolder2 = newPattern("Pattern In Folder Two", folder2);
        patternRepo.save(patternInFolder1);
        patternRepo.save(patternInFolder2);

        List<Pattern> result = patternRepo.findAllByFolderId(folder1.getId());

        assertThat(result)
                .extracting(Pattern::getTitle)
                .containsExactly("Pattern In Folder One");
    }

    @Test
    void findAllByFolderId_returnsEmptyWhenFolderHasNoPatterns() {
        Folder folder = newFolder("Empty Folder");
        folderRepo.save(folder);

        List<Pattern> result = patternRepo.findAllByFolderId(folder.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByFolderId_returnsEmptyWhenFolderIdDoesNotExist() {
        List<Pattern> result = patternRepo.findAllByFolderId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    private Folder newFolder(String name) {
        Folder folder = new Folder();
        folder.setFolderName(name);
        folder.setRef(1);
        folder.setOrigin(PatternOrigin.DRAFTED);
        folder.setLevel(Level.BEGINNER);
        folder.setGarmentType(GarmentType.COURSE);
        return folder;
    }

    private Pattern newPattern(String title, Folder folder) {
        Pattern pattern = new Pattern();
        pattern.setTitle(title);
        pattern.setFolder(folder);
        pattern.setPdfPath("patterns/" + folder.getFolderName() + "/" + title + ".pdf");
        return pattern;
    }
}