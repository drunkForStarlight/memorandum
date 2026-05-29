package com.memoassistant.reports;

import static com.memoassistant.common.DateTimes.parseDate;
import static com.memoassistant.common.DateTimes.parseDateTime;
import static com.memoassistant.common.DateTimes.text;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class WeeklyReportRepository {
    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbc;

    public WeeklyReportRepository(JdbcTemplate jdbcTemplate, JdbcClient jdbc) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbc = jdbc;
    }

    public WeeklyReport create(LocalDate start, LocalDate end, String subject, String content) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO weekly_reports(week_start, week_end, subject, content, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, text(start));
            ps.setString(2, text(end));
            ps.setString(3, subject);
            ps.setString(4, content);
            ps.setString(5, text(now));
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        return findById(id);
    }

    public WeeklyReport markSent(Long id, String recipients) {
        String now = text(LocalDateTime.now());
        jdbc.sql("UPDATE weekly_reports SET sent_to = ?, sent_at = ? WHERE id = ?")
                .params(recipients, now, id)
                .update();
        return findById(id);
    }

    public List<WeeklyReport> findAll() {
        return jdbc.sql("SELECT * FROM weekly_reports ORDER BY created_at DESC")
                .query(this::mapReport)
                .list();
    }

    public WeeklyReport findById(Long id) {
        return jdbc.sql("SELECT * FROM weekly_reports WHERE id = ?")
                .param(id)
                .query(this::mapReport)
                .single();
    }

    private WeeklyReport mapReport(ResultSet rs, int rowNum) throws SQLException {
        return new WeeklyReport(
                rs.getLong("id"),
                parseDate(rs.getString("week_start")),
                parseDate(rs.getString("week_end")),
                rs.getString("subject"),
                rs.getString("content"),
                rs.getString("sent_to"),
                parseDateTime(rs.getString("sent_at")),
                parseDateTime(rs.getString("created_at")));
    }
}
