package com.example.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@SpringBootApplication
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}

@RestController
class DataController {

    @GetMapping("/api/data")
    @PreAuthorize("hasRole('USER')")
    fun getSecureData(): ResponseEntity<Map<String, Any>> {
        val securePayload = mapOf(
            "status" to "Success",
            "message" to "Detta data kommer från en helt säker backend bakom en BFF!",
            "timestamp" to Instant.now().toString(),
            "serverNode" to "Backend-Service-1"
        )
        return ResponseEntity.ok(securePayload)
    }
}
