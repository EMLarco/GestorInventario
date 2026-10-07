package org.example.proyecto1.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final List<Locale> SUPPORTED = List.of(
            Locale.forLanguageTag("es"),
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("pt")
    );

    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es");
    public static final String COOKIE_NAME = "lang";

    @Bean
    public LocaleResolver localeResolver() {
        return new CookieHeaderLocaleResolver();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        registry.addInterceptor(interceptor);
    }

    static class CookieHeaderLocaleResolver implements LocaleResolver {

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            Locale fromCookie = fromCookie(request);
            if (fromCookie != null) {
                return fromCookie;
            }
            Locale fromHeader = firstSupported(Collections.list(request.getLocales()));
            return fromHeader != null ? fromHeader : DEFAULT_LOCALE;
        }

        @Override
        public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
            Locale target = match(locale);
            Cookie cookie = new Cookie(COOKIE_NAME, target.toLanguageTag());
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        private Locale fromCookie(HttpServletRequest request) {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return match(Locale.forLanguageTag(cookie.getValue()));
                }
            }
            return null;
        }

        private Locale firstSupported(List<Locale> requested) {
            for (Locale candidate : requested) {
                Locale matched = matchOrNull(candidate);
                if (matched != null) {
                    return matched;
                }
            }
            return null;
        }

        private Locale match(Locale locale) {
            Locale matched = matchOrNull(locale);
            return matched != null ? matched : DEFAULT_LOCALE;
        }

        private Locale matchOrNull(Locale locale) {
            if (locale == null) {
                return null;
            }
            for (Locale supported : SUPPORTED) {
                if (supported.getLanguage().equalsIgnoreCase(locale.getLanguage())) {
                    return supported;
                }
            }
            return null;
        }

    }

}