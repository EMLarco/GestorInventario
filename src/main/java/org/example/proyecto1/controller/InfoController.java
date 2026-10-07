package org.example.proyecto1.controller;

import org.example.proyecto1.config.AppInfoProperties;
import org.example.proyecto1.dto.AppInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class InfoController {

    private final AppInfoProperties properties;

    public InfoController(AppInfoProperties properties) {
        this.properties = properties;
    }

    @GetMapping({"/info", "/v1/info"})
    public AppInfoResponse info() {
        return new AppInfoResponse(
                properties.getName(),
                properties.getVersion(),
                properties.getDescription(),
                properties.getEnvironment(),
                new AppInfoResponse.DeveloperInfo(
                        properties.getDeveloper().getName(),
                        properties.getDeveloper().getEmail()),
                Instant.now());
    }

}