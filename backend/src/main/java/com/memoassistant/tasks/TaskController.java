package com.memoassistant.tasks;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.memoassistant.auth.AppUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskItem> list(@RequestParam(defaultValue = "active") String view) {
        return taskService.list(view);
    }

    @GetMapping("/{id}")
    public TaskDtos.TaskDetail detail(@PathVariable Long id) {
        return taskService.detail(id);
    }

    @PostMapping
    public TaskItem create(@RequestBody TaskDtos.TaskRequest request, @AuthenticationPrincipal AppUserDetails user) {
        return taskService.create(request, user.id());
    }

    @PutMapping("/{id}")
    public TaskItem update(@PathVariable Long id, @RequestBody TaskDtos.TaskRequest request) {
        return taskService.update(id, request);
    }

    @PostMapping("/{id}/logs")
    public TaskLog addLog(@PathVariable Long id, @RequestBody TaskDtos.LogRequest request,
                          @AuthenticationPrincipal AppUserDetails user) {
        return taskService.addLog(id, request.content(), user.id());
    }

    @PostMapping("/{id}/complete")
    public TaskItem complete(@PathVariable Long id, @RequestBody(required = false) TaskDtos.CompleteRequest request,
                             @AuthenticationPrincipal AppUserDetails user) {
        return taskService.complete(id, request == null ? null : request.summary(), user.id());
    }

    @PostMapping("/{id}/cancel")
    public TaskItem cancel(@PathVariable Long id) {
        return taskService.cancel(id);
    }

    @GetMapping("/export.csv")
    public void exportCsv(HttpServletResponse response) throws Exception {
        String filename = URLEncoder.encode("memo-tasks.csv", StandardCharsets.UTF_8);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename);
        try (PrintWriter writer = response.getWriter()) {
            writer.println('\uFEFF' + "id,title,category,priority,status,dueAt,remindAt,completedAt,createdAt,updatedAt");
            for (TaskItem task : taskService.list("all")) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        task.id(),
                        csv(task.title()),
                        csv(task.category()),
                        task.priority(),
                        task.status(),
                        value(task.dueAt()),
                        value(task.remindAt()),
                        value(task.completedAt()),
                        value(task.createdAt()),
                        value(task.updatedAt()));
            }
        }
    }

    private String value(Object value) {
        return value == null ? "" : csv(value.toString());
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

