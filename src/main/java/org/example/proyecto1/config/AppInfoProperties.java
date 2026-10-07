package org.example.proyecto1.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppInfoProperties {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+.*$", message = "app.version debe ser una version semantica")
    private String version;

    @NotBlank
    private String description;

    @NotBlank
    private String environment;

    @Valid
    private final Developer developer = new Developer();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Developer getDeveloper() {
        return developer;
    }

    public static class Developer {

        @NotBlank
        private String name;

        @NotBlank
        @Email
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

    }

}