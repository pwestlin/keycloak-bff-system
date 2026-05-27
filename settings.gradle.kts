pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Sätter repositories för alla moduler i projektet
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        // Lägg till t.ex. mavenLocal() eller andra publika/privata repos här om det behövs
    }
}
rootProject.name = "keycloak-bff-system"

include("bff-gateway")
include("backend-service")
include("backend-service-client")