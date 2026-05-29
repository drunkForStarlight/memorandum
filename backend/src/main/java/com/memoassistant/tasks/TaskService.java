package com.memoassistant.tasks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskItem> list(String view) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        LocalDateTime soon = today.plusDays(7).atStartOfDay();
        String normalized = view == null ? "active" : view.toLowerCase(Locale.ROOT);

        return taskRepository.findAll().stream()
                .filter(task -> switch (normalized) {
                    case "today" -> task.status() != TaskStatus.DONE
                            && task.status() != TaskStatus.CANCELED
                            && task.dueAt() != null
                            && !task.dueAt().isBefore(start)
                            && task.dueAt().isBefore(end);
                    case "upcoming" -> task.status() != TaskStatus.DONE
                            && task.status() != TaskStatus.CANCELED
                            && task.dueAt() != null
                            && !task.dueAt().isBefore(end)
                            && task.dueAt().isBefore(soon);
                    case "overdue" -> task.status() != TaskStatus.DONE
                            && task.status() != TaskStatus.CANCELED
                            && task.dueAt() != null
                            && task.dueAt().isBefore(LocalDateTime.now());
                    case "completed" -> task.status() == TaskStatus.DONE;
                    case "all" -> true;
                    default -> task.status() == TaskStatus.TODO || task.status() == TaskStatus.DOING;
                })
                .toList();
    }

    public TaskDtos.TaskDetail detail(Long id) {
        TaskItem task = taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        return new TaskDtos.TaskDetail(task, taskRepository.logs(id));
    }

    public TaskItem create(TaskDtos.TaskRequest request, Long userId) {
        validate(request);
        return taskRepository.create(request, userId);
    }

    public TaskItem update(Long id, TaskDtos.TaskRequest request) {
        validate(request);
        taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        return taskRepository.update(id, request);
    }

    @Transactional
    public TaskItem complete(Long id, String summary, Long userId) {
        taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (summary != null && !summary.isBlank()) {
            taskRepository.addLog(id, "完成总结：" + summary.trim(), userId);
        }
        return taskRepository.markStatus(id, TaskStatus.DONE);
    }

    public TaskItem cancel(Long id) {
        taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        return taskRepository.markStatus(id, TaskStatus.CANCELED);
    }

    public TaskLog addLog(Long id, String content, Long userId) {
        taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("处理记录不能为空");
        }
        return taskRepository.addLog(id, content.trim(), userId);
    }

    private void validate(TaskDtos.TaskRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
    }
}

