package org.example.proyecto1.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HelloController.class)
@AutoConfigureMockMvc(addFilters = false)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Muestra la frase por defecto en espanol")
    void shouldShowDefaultGreeting() throws Exception {
        mockMvc.perform(get("/").locale(Locale.forLanguageTag("es")))
                .andExpect(status().isOk())
                .andExpect(view().name("hello"))
                .andExpect(model().attribute("message", "Hola Mundo"))
                .andExpect(model().attribute("currentLang", "es"));
    }

    @Test
    @DisplayName("Muestra la frase con el nombre del parametro y el idioma en ingles")
    void shouldShowGreetingWithNameParameter() throws Exception {
        mockMvc.perform(get("/home").param("name", "Ana").locale(Locale.forLanguageTag("en")))
                .andExpect(status().isOk())
                .andExpect(view().name("hello"))
                .andExpect(model().attribute("message", "Hello Ana"))
                .andExpect(model().attribute("currentLang", "en"));
    }

}