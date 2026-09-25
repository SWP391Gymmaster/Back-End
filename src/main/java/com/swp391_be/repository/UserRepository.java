package com.swp391_be.repository;

import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUserName(String userName);
    List<User> findByRole(Role role);
    boolean existsByIdAndRole(Long id, Role role);
}
