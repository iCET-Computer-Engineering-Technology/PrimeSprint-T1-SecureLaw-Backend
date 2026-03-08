package com.primesprint.repository;

import com.primesprint.model.User;

public interface UserRepository {

    User findByUsernameOrEmail(String value);
}
