package com.primesprint.repository;

import com.primesprint.model.entity.Role;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    UUID findRoleIdByName(String roleName);

    Optional<Role> findByName(String name);
}
