package br.com.agendasemfuro.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            @Value("${server.servlet.session.cookie.secure:true}") boolean secureCookies
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.path("/").secure(secureCookies).sameSite("Strict"));

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) ->
                        writeProblem(objectMapper, response, HttpStatus.UNAUTHORIZED,
                                "Authentication required", "Sign in to use this resource."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeProblem(objectMapper, response, HttpStatus.FORBIDDEN,
                                        "Access denied", "The request is not allowed or its security token is invalid.")))
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((request, response, exception) ->
                                writeProblem(objectMapper, response, HttpStatus.UNAUTHORIZED,
                                        "Login failed", "The username or password is incorrect."))
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService adminUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin.username}") String username,
            @Value("${app.security.admin.password}") String password
    ) {
        Assert.hasText(username, "Set ADMIN_USERNAME to configure the panel administrator.");
        Assert.hasText(password, "Set ADMIN_PASSWORD to configure the panel administrator.");
        Assert.isTrue(password.length() >= 16, "ADMIN_PASSWORD must contain at least 16 characters.");

        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build());
    }

    private static void writeProblem(
            ObjectMapper objectMapper,
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           Supplier<CsrfToken> deferredCsrfToken) {
            xor.handle(request, response, deferredCsrfToken);
            deferredCsrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return headerValue == null ? xor.resolveCsrfTokenValue(request, csrfToken)
                    : plain.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
