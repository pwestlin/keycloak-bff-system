# OAuth2 & OIDC Multi-Module System

Detta projekt är en robust, modern och produktionsklar säkerhetsarkitektur uppdelad i ett Spring Boot-baserat multi-modulprojekt. Systemet demonstrerar hur man säkrar en applikation med hjälp av **Identity and Access Management (IAM)** via Keycloak, genom att helt separera sessionshantering från tokenvalidering och maskin-till-maskin-kommunikation.

---

## 1. Generell beskrivning av säkerhetslösningen

Säkerhetsarkitekturen bygger på principen om **decentraliserad säkerhet** och mönstret **BFF (Backend-for-Frontend)** kombinerat med **OAuth2 och OpenID Connect (OIDC)**. 

### Kärnprinciper:
* **Inga tokens i webbläsaren (XSS-skydd):** Den traditionella (och osäkra) metoden att spara JWTs (Access Tokens) i webbläsarens `localStorage` eller `sessionStorage` undviks helt. Webbläsaren ser eller hanterar aldrig några kryptografiska tokens.
* **Cookie-baserad session:** Kommunikationen mellan JavaScript-klienten och API-gatewayen (BFF) sker via säkra, krypterade HTTP-only och SameSite-cookies.
* **Token-baserad backend:** Kommunikationen bakom gatewayen är helt tillståndslös (stateless) och baseras på standardiserade JWT-tokens (Bearer tokens). 
* **Tydlig ansvarsfördelning (Separation of Concerns):** Spring-applikationerna hanterar aldrig användarnas lösenord. Keycloak äger inloggningssidan och användardatabasen, gatewayen äger sessionen, och backenden äger resursen.

---

## 2. Modularkitektur och säkerhetsflöden

Projektet är uppdelat i tre Gradle-moduler under en gemensam rot. Samtliga moduler körs på **Java 21 (LTS)** för optimalt verktygsstöd och prestanda.

### 🏢 bff-gateway (Port 8080)
* **Ansvar:** Agerar applikationens API-gateway och **Backend-for-Frontend (BFF)**. Den serverar de statiska frontend-filerna (HTML/JS) och fungerar som den centrala dörrvakten för all inkommande API-trafik (/api/**).
* **Säkerhetsflöde:** **Authorization Code Grant med PKCE (Proof Key for Code Exchange)**.
* **Varför?** Detta är branschstandard (RFC 7636) för webbapplikationer. När en oautentiserad användare anropar ett skyddat API svarar gatewayen med `401 Unauthorized`. Frontend omdirigerar då hela webbläsaren till Keycloaks inloggningssida. Efter godkänd inloggning byter gatewayen (på serversidan) inloggningskoden mot en Access Token via sin konfidentiella `client-secret`. Denna token sparas i gatewayens serversession, och skickas vidare till backenden via et `TokenRelay`-filter.

### ⚙️ backend-service (Port 8181)
* **Ansvar:** Fungerar som applikationens kärn-API (Resource Server) som innehåller affärslogik och skyddat data (/api/data). 
* **Säkerhetsflöde:** **OAuth2 Resource Server (JWT Validering)**.
* **Varför?** Denna tjänst är helt tillståndslös och vet ingenting om cookies, sessioner eller hur användaren loggade in. Den litar blint på den `Authorization: Bearer <JWT>`-header som skickas med i anropen. Vid varje anrop validerar den tokens signatur i minnet mot Keycloaks publika nycklar, samt kontrollerar att token innehåller rätt roll (t.ex. `USER`) innan den släpper igenom anropet.
* **Kontrollerar inte bara var JWT:n kommer från utan också dess `audience`**:
```yaml
audiences:
  - backend-client-service
  - web-bff-client
```

### 🤖 backend-service-client (Port 8282)
* **Ansvar:** Demonstrerar autonom maskin-till-maskin-kommunikation (Backend-to-Backend). Det är en bakgrundstjänst/klient som behöver hämta data från `backend-service` helt utan mänsklig interaktion.
* **Säkerhetsflöde:** **Client Credentials Grant**.
* **Varför?** Eftersom det inte finns någon mänsklig användare eller webbläsare inblandad kan vi inte visa ett inloggningsformulär. Istället använder denna modul sin egen unika `client-id` och `client-secret` för att autentisera sig direkt mot Keycloaks token-endpoint i bakgrunden. Keycloak utfärdar en maskin-JWT (kopplad till ett *Service Account* med rollen `USER`) som en konfigurerad `RestClient` automatiskt bifogar i anropen till backenden.

---

## 3. Exekveringsinstruktioner

Följ dessa steg i ordning för att starta upp hela den lokala miljön.

### Steg 1: Starta Keycloak
Kör följande kommando i din terminal för att starta Keycloak i utvecklingsläge. Kommandot mappar datakatalogen till din lokala maskin så att dina inställningar sparas.

```bash
docker run --name keycloak -d \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -p 9000:8080 \
  -v "$(pwd)/keycloak_data:/opt/keycloak/data" \
  quay.io/keycloak/keycloak:latest start-dev
  ```
* **Konfiguration:**
Keycloak är färdigkonfigurerad och konfigurationen finns lagrad i [keycloak_data](keycloak_data).  
* **Administrationsgränssnitt:** Surfa till http://localhost:9000 och logga in med admin/admin.

### Steg 2: Starta applikationerna via Gradle
Öppna separata terminalfönster (eller flikar i din IDE) och exekvera följande kommandon från projektets rotkatalog:

#### 1. Starta Backend (Resource Server)
```bash
./gradlew :backend-service:bootRun
```
Lyssnar på port 8181. Verifiera i loggarna att den startat.

#### 2. Starta API Gateway / BFF
```bash
./gradlew :bff-gateway:bootRun
```
Lyssnar på port 8080. Det är denna adress http://localhost:8080 du besöker i webbläsaren för att testa applikationen.

#### 3. Starta Backend Client (Maskin-till-maskin)
```bash
./gradlew :backend-service-client:bootRun
```
Tjänsten körs, hämtar automatiskt en token från Keycloak, anropar backenden på port 8181 och loggar resultatet i konsolen.

---
