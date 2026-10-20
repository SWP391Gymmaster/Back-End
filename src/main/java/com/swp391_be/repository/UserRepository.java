package com.swp391_be.repository;

import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);
    boolean existsByUserNameIgnoreCase(String userName);
    List<User> findByRole(Role role);
    boolean existsByIdAndRole(Long id, Role role);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
    @Query("select u from User u where u.role <> com.swp391_be.entity.Role.ADMIN " +
            "and (:role is null or u.role = :role) and (:enabled is null or u.enabled = :enabled) " +
            "and (:verified is null or u.emailVerified = :verified) " +
            "and (:q is null or lower(u.userName) like lower(:q) escape '!' " +
            "or lower(u.fullName) like lower(:q) escape '!' or lower(u.email) like lower(:q) escape '!') " +
            "order by u.id desc")
    Page<User> searchManagedUsers(@Param("q") String query, @Param("role") Role role,
            @Param("enabled") Boolean enabled, @Param("verified") Boolean verified, Pageable pageable);
}
