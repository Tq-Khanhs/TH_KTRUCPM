package iuh.fit.se.apigateway.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r.path("/products/**")
                        .uri("http://product-service:8081"))
                .route("order-service", r -> r.path("/orders/**")
                        .uri("http://order-service:8082"))
                .route("customer-service", r -> r.path("/customers/**")
                        .uri("http://customer-service:8083"))
                .build();
    }
}
