package org.example.proyecto1.dto;

import java.time.Instant;

public record AppInfoResponse(
        String name,
        String version,
        String description,
        String environment,
        DeveloperInfo developer,
        Instant timestamp
) {

    public record DeveloperInfo(String name, String email) {
    }

}