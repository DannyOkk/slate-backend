package com.slate.backend.repository;

import com.slate.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByPhoneOrNameContainingIgnoreCase(String phone, String name);
}
