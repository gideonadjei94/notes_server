package com.gideon.notes.repository;

import com.gideon.notes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
}
