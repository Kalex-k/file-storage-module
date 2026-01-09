package com.filestorage.repository;

import com.filestorage.model.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    @Modifying
    @Query("UPDATE Project p SET p.storageSize = :size WHERE p.id = :id")
    int updateStorageSize(
            @Param("id") Long id,
            @Param("size") java.math.BigInteger size
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdWithLock(@Param("id") Long id);
}
