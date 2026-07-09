package com.xinyue.atelier.repository;

import com.xinyue.atelier.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderRepo extends JpaRepository<Folder, UUID> {
    List<Folder> findByParentFolderIsNull();
    List<Folder> findByParentFolderId(UUID parentId);
}
