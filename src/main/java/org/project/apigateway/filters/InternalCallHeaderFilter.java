package org.project.apigateway.filters;

import org.project.apigateway.util.InternalTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Component
public class InternalCallHeaderFilter implements GlobalFilter, Ordered {

    @Value("${internal.token.secret}")
    private String secret;

    private static final String HEADER_NAME = "X-Internal-Token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        if (exchange.getRequest().getURI().getPath().startsWith("/fallback/")) {
            String token = InternalTokenUtil.generateToken(secret);

            var mutatedRequest = exchange.getRequest().mutate()
                    .header(HEADER_NAME, token)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // Run early
    }
}
