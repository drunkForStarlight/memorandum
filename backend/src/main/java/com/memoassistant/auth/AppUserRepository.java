package com.memoassistant.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepository {
    private final JdbcClient jdbc;

    public AppUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByUsername(String username) {
        return jdbc.sql("SELECT * FROM app_users WHERE username = ?")
                .param(username)
                .query(this::mapUser)
                .optional();
    }

    public Optional<AppUser> findById(Long id) {
        return jdbc.sql("SELECT * FROM app_users WHERE id = ?")
                .param(id)
                .query(this::mapUser)
                .optional();
    }

    private AppUser mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new AppUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                rs.getString("role"),
                rs.getInt("enabled") == 1,
                LocalDateTime.parse(rs.getString("created_at")),
                LocalDateTime.parse(rs.getString("updated_at")));
    }
}

