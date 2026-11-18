package com.lamar.backend.controller;

import com.lamar.backend.dto.DashboardDto;
import com.lamar.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen")
    public DashboardDto resumen(@RequestParam(defaultValue = "30") int dias) {
        return dashboardService.resumen(dias);
    }
}