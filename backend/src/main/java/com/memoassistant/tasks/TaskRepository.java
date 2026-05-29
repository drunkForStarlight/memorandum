package com.memoassistant.tasks;

import static com.memoassistant.common.DateTimes.parseDateTime;
import static com.memoassistant.common.DateTimes.text;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {
    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbc;

    public TaskRepository(JdbcTemplate jdbcTemplate, JdbcClient jdbc) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbc = jdbc;
    }

    public List<TaskItem> findAll() {
        return jdbc.sql("SELECT * FROM tasks ORDER BY updated_at DESC")
                .query(this::mapTask)
                .list();
    }

    public Optional<TaskItem> findById(Long id) {
        return jdbc.sql("SELECT * FROM tasks WHERE id = ?")
                .param(id)
                .query(this::mapTask)
                .optional();
    }

    public TaskItem create(TaskDtos.TaskRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement("""
                    INSERT INTO tasks(title, description, category, priority, status, due_at, remind_at, created_by, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.title());
            ps.setString(2, request.description());
            ps.setString(3, request.category());
            ps.setString(4, priority(request.priority()).name());
            ps.setString(5, status(request.status()).name());
            ps.setString(6, text(request.dueAt()));
            ps.setString(7, text(request.remindAt()));
            ps.setLong(8, userId);
            ps.setString(9, text(now));
            ps.setString(10, text(now));
            return ps;
        }, keyHolder);
        Long id = keyHolder.getKey().longValue();
        return findById(id).orElseThrow();
    }

    public TaskItem update(Long id, TaskDtos.TaskRequest request) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.sql("""
                UPDATE tasks
                SET title = ?, description = ?, category = ?, priority = ?, status = ?, due_at = ?, remind_at = ?,
                    reminder_sent_at = NULL,
                    updated_at = ?
                WHERE id = ?
                """)
                .params(
                        request.title(),
                        request.description(),
                        request.category(),
                        priority(request.priority()).name(),
                        status(request.status()).name(),
                        text(request.dueAt()),
                        text(request.remindAt()),
                        text(now),
                        id)
                .update();
        return findById(id).orElseThrow();
    }

    public TaskItem markStatus(Long id, TaskStatus status) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.sql("""
                UPDATE tasks
                SET status = ?, completed_at = ?, updated_at = ?
                WHERE id = ?
                """)
                .params(status.name(), status == TaskStatus.DONE ? text(now) : null, text(now), id)
                .update();
        return findById(id).orElseThrow();
    }

    public void markReminderSent(Long id) {
        String now = text(LocalDateTime.now());
        jdbc.sql("UPDATE tasks SET reminder_sent_at = ?, updated_at = ? WHERE id = ?")
                .params(now, now, id)
                .update();
    }

    public List<TaskItem> dueReminders(LocalDateTime now) {
        return jdbc.sql("""
                SELECT * FROM tasks
                WHERE remind_at IS NOT NULL
                  AND reminder_sent_at IS NULL
                  AND status IN ('TODO', 'DOING')
                  AND remind_at <= ?
                ORDER BY remind_at ASC
                """)
                .param(text(now))
                .query(this::mapTask)
                .list();
    }

    public List<TaskLog> logs(Long taskId) {
        return jdbc.sql("SELECT * FROM task_logs WHERE task_id = ? ORDER BY created_at DESC")
                .param(taskId)
                .query(this::mapLog)
                .list();
    }

    public TaskLog addLog(Long taskId, String content, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    "INSERT INTO task_logs(task_id, content, created_by, created_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId);
            ps.setString(2, content);
            ps.setLong(3, userId);
            ps.setString(4, text(now));
            return ps;
        }, keyHolder);
        jdbc.sql("UPDATE tasks SET updated_at = ? WHERE id = ?").params(text(now), taskId).update();
        Long id = keyHolder.getKey().longValue();
        return jdbc.sql("SELECT * FROM task_logs WHERE id = ?")
                .param(id)
                .query(this::mapLog)
                .single();
    }

    private TaskItem mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new TaskItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                TaskPriority.valueOf(rs.getString("priority")),
                TaskStatus.valueOf(rs.getString("status")),
                parseDateTime(rs.getString("due_at")),
                parseDateTime(rs.getString("remind_at")),
                parseDateTime(rs.getString("reminder_sent_at")),
                parseDateTime(rs.getString("completed_at")),
                rs.getLong("created_by"),
                parseDateTime(rs.getString("created_at")),
                parseDateTime(rs.getString("updated_at")));
    }

    private TaskLog mapLog(ResultSet rs, int rowNum) throws SQLException {
        return new TaskLog(
                rs.getLong("id"),
                rs.getLong("task_id"),
                rs.getString("content"),
                rs.getLong("created_by"),
                parseDateTime(rs.getString("created_at")));
    }

    private TaskPriority priority(TaskPriority priority) {
        return priority == null ? TaskPriority.MEDIUM : priority;
    }

    private TaskStatus status(TaskStatus status) {
        return status == null ? TaskStatus.TODO : status;
    }
}
