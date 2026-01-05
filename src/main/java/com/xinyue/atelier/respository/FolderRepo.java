package com.xinyue.atelier.respository;

import com.xinyue.atelier.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepo extends JpaRepository<Folder, Long> {

    @Query("select f.folderName from Folder f")
    List<String> findAllFolderNames();
}
