package com.xinyue.atelier.repository;

import com.xinyue.atelier.GarmentType;
import com.xinyue.atelier.Level;
import com.xinyue.atelier.PatternOrigin;
import com.xinyue.atelier.model.Folder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

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

    @Test
    void findByParentFolderId_returnsOnlyDirectChildrenOfGivenParent() {
        Folder root1 = newFolder("Root One", null);
        Folder root2 = newFolder("Root Two", null);
        folderRepo.save(root1);
        folderRepo.save(root2);

        Folder child1 = newFolder("Child One", root1);
        Folder child2 = newFolder("Child Two", root1);
        Folder otherChild = newFolder("Other Child", root2);
        folderRepo.save(child1);
        folderRepo.save(child2);
        folderRepo.save(otherChild);

        List<Folder> result = folderRepo.findByParentFolderId(root1.getId());

        assertThat(result)
                .extracting(Folder::getFolderName)
                .containsExactlyInAnyOrder("Child One", "Child Two");
    }

    @Test
    void findByParentFolderId_excludesGrandchildren() {
        Folder root = newFolder("Root", null);
        folderRepo.save(root);

        Folder child = newFolder("Child", root);
        folderRepo.save(child);

        Folder grandchild = newFolder("Grandchild", child);
        folderRepo.save(grandchild);

        List<Folder> result = folderRepo.findByParentFolderId(root.getId());

        assertThat(result)
                .extracting(Folder::getFolderName)
                .containsExactly("Child");
    }

    @Test
    void findByParentFolderId_returnsEmptyWhenParentHasNoChildren() {
        Folder root = newFolder("Root", null);
        folderRepo.save(root);

        List<Folder> result = folderRepo.findByParentFolderId(root.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByParentFolderId_returnsEmptyWhenParentIdDoesNotExist() {
        List<Folder> result = folderRepo.findByParentFolderId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }
}