package org.project.apigateway.ratelimiters.keyresolvers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class RateLimiterKeyResolver {
    private final JwtParser jwtParser;

    public RateLimiterKeyResolver(@Value("${jwt.secret}") String secret) {
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            HttpCookie jwtCookie = exchange.getRequest().getCookies().getFirst("JWT_TOKEN");
            if (jwtCookie == null) return Mono.just("anonymous");

            return Mono.fromSupplier(() -> {
                try {
                    Claims claims = jwtParser.parseClaimsJws(jwtCookie.getValue()).getBody();
                    String subject = claims.getSubject();
                    return subject != null ? subject : "anonymous";
                } catch (Exception e) {
                    return "anonymous";
                }
            });
        };
    }
}
