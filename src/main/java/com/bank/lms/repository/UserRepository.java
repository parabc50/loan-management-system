package com.bank.lms.repository;

import com.bank.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByCustomerId(Long customerId);
    List<User> findByRole(String role);
    boolean existsByUsername(String username);
}