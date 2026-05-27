package com.example.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
                        "/static/**", "/js/**", "/api/me"
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

@RestController
@RequestMapping("/api")
class UserController {

    @GetMapping("/me")
    fun currentUser(@AuthenticationPrincipal principal: OAuth2User?): ResponseEntity<Any> {
        return if (principal != null) {
            // Returnera grundläggande info eller bara 200 OK
            ResponseEntity.ok(mapOf("authenticated" to true, "name" to principal.attributes["preferred_username"]))
        } else {
            // Returnera 401 så att frontenden vet att användaren inte är inloggad
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}