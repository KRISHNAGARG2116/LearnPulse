package com.learnpulse.backend.repository;

import com.learnpulse.backend.entity.Role;
import com.learnpulse.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRoleAndEnabled(Role role, boolean enabled);

    long countByRole(Role role);

    long countByEnabled(boolean enabled);

    Page<User> findByRoleAndEnabled(Role role, boolean enabled, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = true")
    List<User> findActiveAdminsForUpdate(@Param("role") Role role);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) " +
           "AND (:enabled IS NULL OR u.enabled = :enabled) " +
           "AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersWithKeyword(@Param("role") Role role,
                                      @Param("enabled") Boolean enabled,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query(value = "SELECT CAST(u.created_at AS date) AS reg_date, COUNT(*) AS count " +
                   "FROM users u " +
                   "WHERE u.created_at >= :startDate " +
                   "GROUP BY CAST(u.created_at AS date) " +
                   "ORDER BY reg_date ASC", nativeQuery = true)
    List<Object[]> countRegistrationsByDayNative(@Param("startDate") java.time.Instant startDate);
}
