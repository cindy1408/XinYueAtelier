package com.xinyue.atelier.repository;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FolderRepoTest {

    @Autowired
    private FolderRepo folderRepo;

    @Test
    void findByParentFolderIsNull_returnsOnlyRootFolders() {
        Folder root1 = newFolder("Root One", null);
        Folder root2 = newFolder("Root Two", null);
        Folder child = newFolder("Child", root1);

        folderRepo.save(root1);
        folderRepo.save(root2);
        folderRepo.save(child);

        List<Folder> result = folderRepo.findByParentFolderIsNull();

        assertThat(result)
                .extracting(Folder::getFolderName)
                .containsExactlyInAnyOrder("Root One", "Root Two");
    }

    @Test
    void findByParentFolderIsNull_returnsEmptyWhenNoRootFolders() {
        List<Folder> result = folderRepo.findByParentFolderIsNull();

        assertThat(result).isEmpty();
    }

    private Folder newFolder(String name, Folder parent) {
        Folder folder = new Folder();
        folder.setFolderName(name);
        folder.setRef(1);
        folder.setOrigin(PatternOrigin.DRAFTED);
        folder.setLevel(Level.BEGINNER);
        folder.setGarmentType(GarmentType.COURSE);
        folder.setParentFolder(parent);
        return folder;
    }
}