package org.project.apigateway.config;

import org.project.apigateway.util.InternalTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${internal.token.secret}")
    private String secret;

    private static final long ALLOWED_WINDOW_SECONDS = 30;
    private static final String HEADER_NAME = "X-Internal-Token";

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        http
                .authorizeExchange(exchanges -> exchanges
                        // ✅ Actuator must be FIRST
                        .pathMatchers("/actuator/**").permitAll()

                        // ✅ Internal fallback protected by internal token
                        .pathMatchers("/fallback/**")
                        .access((mono, context) -> mono.map(auth ->
                                new AuthorizationDecision(
                                        InternalTokenUtil.validateToken(
                                                context.getExchange().getRequest().getHeaders().getFirst(HEADER_NAME),
                                                secret,
                                                ALLOWED_WINDOW_SECONDS
                                        )
                                )
                        ))
                        .anyExchange().permitAll()
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }
}

