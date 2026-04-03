package com.primesprint.repository.impl;

import com.primesprint.mapper.ProfileRowMapper;
import com.primesprint.model.entity.Profile;
import com.primesprint.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UUID createProfile(UUID userID, String displayName) {

        UUID profileId = UUID.randomUUID();

        String sql = """
               INSERT INTO profiles (id, userId, displayName,createdAt)
               VALUES (?,?,?,now())
           """;

        jdbcTemplate.update(
                sql,
                profileId,
                userID,
                displayName
        );
        return profileId;
    }

    @Override
    public Profile getProfile(UUID userID) {

        String sql = """
                SELECT * FROM profiles
                WHERE userId = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new ProfileRowMapper(),
                userID
        );
    }
}
