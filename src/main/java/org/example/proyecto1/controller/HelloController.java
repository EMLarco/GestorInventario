package org.example.proyecto1.controller;

import org.example.proyecto1.config.WebConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HelloController {

    public static final Map<String, String> LANGUAGES = WebConfig.SUPPORTED.stream()
            .collect(Collectors.toMap(Locale::getLanguage, l -> switch (l.getLanguage()) {
                case "es" -> "Espa\u00f1ol";
                case "en" -> "English";
                case "pt" -> "Portugu\u00eas";
                default -> l.getLanguage();
            }, (a, b) -> a, LinkedHashMap::new));

    private final MessageSource messageSource;

    public HelloController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping({"/", "/home"})
    public String hello(@RequestParam(value = "name", required = false) String name, Model model) {
        Locale locale = LocaleContextHolder.getLocale();
        String target = (name == null || name.isBlank()) ? messageSource.getMessage("home.world", null, locale) : name;
        String message = messageSource.getMessage("home.greeting", new Object[]{target}, locale);

        model.addAttribute("message", message);
        model.addAttribute("name", name);
        model.addAttribute("currentLang", locale.getLanguage());
        model.addAttribute("languages", LANGUAGES);
        return "hello";
    }

    @GetMapping(value = "/home/text", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String helloText(@RequestParam(value = "name", required = false) String name) {
        Locale locale = LocaleContextHolder.getLocale();
        String target = (name == null || name.isBlank()) ? messageSource.getMessage("home.world", null, locale) : name;
        return messageSource.getMessage("home.greeting", new Object[]{target}, locale);
    }

}
