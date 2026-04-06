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
    public UUID createProfile(UUID userId, String displayName) {

        UUID profileId = UUID.randomUUID();

        String sql = """
               INSERT INTO profiles (id, user_id, display_name,created_at)
               VALUES (?,?,?,now())
           """;

        jdbcTemplate.update(
                sql,
                profileId,
                userId,
                displayName
        );

        return profileId;
    }

    @Override
    public Profile getProfile(UUID userID) {

        String sql = """
                SELECT * FROM profiles
                WHERE user_id = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new ProfileRowMapper(),
                userID
        );
    }
}
