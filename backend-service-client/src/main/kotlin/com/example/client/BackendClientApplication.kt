package com.example.client

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Configuration
class OAuth2ClientConfig {

    // 1. Skapa en manager som automatiskt vet hur man hanterar client_credentials
    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService
    ): OAuth2AuthorizedClientManager {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials() // Aktiverar maskin-till-maskin hantering
            .build()

        val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService
        )
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
        return authorizedClientManager
    }

    // 2. Skapa en färdig RestClient som automatiskt lägger till JWT-token vid varje anrop
    @Bean
    fun backendServiceClient(authorizedClientManager: OAuth2AuthorizedClientManager): RestClient {
        return RestClient.builder()
            .baseUrl("http://localhost:8181") // Bas-URL till din skyddade backend-service
            .requestInterceptor { request, body, execution ->
                // Vi hämtar token för vår registrering "backend-to-backend" som vi namngav i YAML
                val authorizeRequest = org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
                    .withClientRegistrationId("backend-to-backend")
                    .principal("backend-service-client")
                    .build()

                val authorizedClient = authorizedClientManager.authorize(authorizeRequest)
                val token = authorizedClient?.accessToken?.tokenValue

                if (token != null) {
                    request.headers.setBearerAuth(token)
                }
                execution.execute(request, body)
            }
            .build()
    }
}

@SpringBootApplication
class BackendServiceClientApplication

fun main(args: Array<String>) {
    runApplication<BackendServiceClientApplication>(*args)
}

@Service
class DataFetcherService(private val backendServiceClient: RestClient) : CommandLineRunner {

    override fun run(vararg args: String) {
        println("Försöker hämta data maskin-till-maskin från backend-service...")

        try {
            // Gör anropet till din skyddade endpoint /api/data
            val result = backendServiceClient.get()
                .uri("/api/data")
                .retrieve()
                .body<String>()

            println("\n🎉 SUCCESS! Data mottagen från backend via maskin-token:\n$result\n")
        } catch (e: Exception) {
            println("\n❌ Misslyckades med att hämta data: ${e.message}\n")
        }
    }
}