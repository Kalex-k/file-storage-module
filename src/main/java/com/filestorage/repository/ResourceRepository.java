package com.filestorage.repository;

import com.filestorage.model.Resource;
import com.filestorage.model.ResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.project.id = :projectId AND r.status = :status")
    Page<Resource> findByProjectIdAndStatus(
            @Param("projectId") Long projectId,
            @Param("status") ResourceStatus status,
            Pageable pageable
    );

    List<Resource> findByProjectIdAndCreatedById(Long projectId, Long memberId);

    Optional<Resource> findByKey(String key);

    @Query("SELECT r FROM Resource r WHERE r.id = :resourceId AND r.project.id = :projectId")
    Optional<Resource> findByIdAndProjectId(
            @Param("resourceId") Long resourceId,
            @Param("projectId") Long projectId
    );

    @Query("SELECT SUM(r.size) FROM Resource r WHERE r.project.id = :projectId AND r.status = 'ACTIVE'")
    Long calculateProjectStorageSize(@Param("projectId") Long projectId);

    @Modifying
    @Query("UPDATE Resource r SET r.status = :status WHERE r.id = :id")
    int updateStatus(
            @Param("id") Long id,
            @Param("status") ResourceStatus status
    );
}
