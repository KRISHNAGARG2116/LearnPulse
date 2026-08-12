package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.InfrastructureCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InfrastructureCheckRepository extends JpaRepository<InfrastructureCheckEntity, Long> {

    @Query(value = "SELECT COUNT(*) > 0 FROM pg_extension WHERE extname = 'vector'", nativeQuery = true)
    boolean isPgVectorInstalled();

    @Query(value = "SELECT extversion FROM pg_extension WHERE extname = 'vector'", nativeQuery = true)
    String getPgVectorVersion();
}
