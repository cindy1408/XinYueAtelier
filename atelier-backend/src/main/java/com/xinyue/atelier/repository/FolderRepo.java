package com.xinyue.atelier.repository;

import com.xinyue.atelier.model.Folder;
import com.xinyue.atelier.model.GarmentType;
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
    SELECT f.ref FROM Folder f
    WHERE f.ref IS NOT NULL AND f.garmentType = :type
    ORDER BY f.ref
""")
    List<Integer> findUsedRefsByType(@Param("type") GarmentType type);

    @Query("""
    SELECT f.ref FROM Folder f
    WHERE f.ref IS NOT NULL AND f.garmentType <> :type
    ORDER BY f.ref
""")
    List<Integer> findUsedRefsExcludingType(@Param("type") GarmentType type);

    default List<Integer> findAllUsedRefsByGroup(boolean course) {
        return course
                ? findUsedRefsByType(GarmentType.COURSE)
                : findUsedRefsExcludingType(GarmentType.COURSE);
    }
}
