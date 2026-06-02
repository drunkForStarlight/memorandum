package com.memoassistant.config;

import jakarta.servlet.DispatcherType;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoassistant.auth.AppUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppUserDetailsService userDetailsService) throws Exception {
        http.userDetailsService(userDetailsService);
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers("/api/auth/login"));
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                .requestMatchers(
                        new AntPathRequestMatcher("/api/auth/login"),
                        new AntPathRequestMatcher("/api/auth/csrf"))
                .permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                .anyRequest().permitAll());
        http.formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> writeJson(response, 200, new SimpleResponse(true, "登录成功")))
                .failureHandler((request, response, exception) -> writeJson(response, 401, new SimpleResponse(false, "用户名或密码不正确"))));
        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> writeJson(response, 200, new SimpleResponse(true, "已退出"))));
        http.httpBasic(Customizer.withDefaults());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                writeJson(response, 401, new SimpleResponse(false, "请先登录"))));
        return http.build();
    }

    private static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), body);
    }

    record SimpleResponse(boolean ok, String message) {
    }
}
