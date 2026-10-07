package org.example.proyecto1.controller;

import org.example.proyecto1.config.AppInfoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InfoController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableConfigurationProperties(AppInfoProperties.class)
class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/info devuelve 200 y la estructura esperada")
    void shouldReturnAppInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gestor de Inventario"))
                .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.environment").isNotEmpty())
                .andExpect(jsonPath("$.developer.name").isNotEmpty());
    }

    @Test
    @DisplayName("El email del developer tiene formato valido")
    void shouldReturnValidDeveloperEmail() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.developer.email")
                        .value(matchesPattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")));
    }

    @Test
    @DisplayName("El endpoint versionado /api/v1/info responde igual")
    void shouldExposeVersionedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gestor de Inventario"));
    }

}