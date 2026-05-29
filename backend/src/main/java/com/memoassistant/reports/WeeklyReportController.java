package com.memoassistant.reports;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/weekly")
public class WeeklyReportController {
    private final WeeklyReportService reportService;

    public WeeklyReportController(WeeklyReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<WeeklyReport> list() {
        return reportService.list();
    }

    @PostMapping("/generate")
    public WeeklyReport generate() {
        return reportService.generateCurrentWeek();
    }

    @PostMapping("/{id}/send")
    public WeeklyReport send(@PathVariable Long id) {
        return reportService.send(id);
    }
}

