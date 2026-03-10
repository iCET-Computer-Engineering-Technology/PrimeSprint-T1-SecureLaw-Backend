package com.primesprint.repository;

import java.util.UUID;

public interface RoleRepository {

    UUID findRoleIdByName(String roleName);
}
