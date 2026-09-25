package com.xinyue.atelier.repository;

import com.xinyue.atelier.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderRepo extends JpaRepository<Folder, UUID> {
    List<Folder> findByParentFolderIsNull();

    List<Folder> findByParentFolderId(UUID parentId);

    @Query("""
    SELECT f.ref
    FROM Folder f
    WHERE f.ref IS NOT NULL
    AND (
        (:course = true AND f.garmentType = com.xinyue.atelier.GarmentType.COURSE)
        OR
        (:course = false AND f.garmentType <> com.xinyue.atelier.GarmentType.COURSE)
    )
    ORDER BY f.ref
""")
    List<Integer> findAllUsedRefsByGroup(
            @Param("course") boolean course
    );
}
