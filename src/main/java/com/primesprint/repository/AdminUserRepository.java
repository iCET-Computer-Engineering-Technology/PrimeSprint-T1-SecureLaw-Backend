package com.primesprint.repository;

import com.primesprint.model.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository {
    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    User save(User user);

    boolean existsBySeniorId(UUID seniorId);

    Optional<User> findById(UUID id);

    User update(User user);

    void delete(UUID id);

    List<User> findAll(int offset, int size, String sort, String direction);

    long count(String search);

    long countAll();

    List<User> search(int offset, int size, String sort, String direction, String search);
}
