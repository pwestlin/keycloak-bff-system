package com.example.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint

@SpringBootApplication
class GatewayApplication

fun main(args: Array<String>) {
    runApplication<GatewayApplication>(*args)
}

@Configuration
@EnableWebFluxSecurity
class GatewaySecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .authorizeExchange { exchange ->
                exchange
                    // Tillåt startsidan, din app.js, favicons och eventuella undermappar utan inloggning
                    .pathMatchers(
                        "/", "/index.html", "/app.js", "/favicon.ico",
                        "/login/oauth2/code/keycloak", // <--- ÄR DENNA MED?
                        "/static/**", "/js/**"
                    ).permitAll()
                    // Allt annat (som dina API-anrop under /api/**) kräver inloggning
                    .anyExchange().authenticated()
            }
            .oauth2Login { }
            .exceptionHandling { exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            }

        return http.build()
    }
}
