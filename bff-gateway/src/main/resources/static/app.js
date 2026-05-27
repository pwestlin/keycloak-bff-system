const actionBtn = document.getElementById('actionBtn');
const output = document.getElementById('output');
const status = document.getElementById('status');

// 1. Kontrollera status vid start
async function initApp() {
    try {
        const response = await fetch('/api/me');
        if (response.ok) {
            const user = await response.json();
            status.innerText = `Inloggad som: ${user.name}`;
            actionBtn.innerText = "Hämta data från skyddad backend";
            actionBtn.onclick = fetchData; // Koppla till data-hämtning
        } else {
            status.innerText = "Du är inte inloggad.";
            actionBtn.innerText = "Logga in";
            actionBtn.onclick = () => {
                window.location.href = '/oauth2/authorization/keycloak-bff';
            };
        }
    } catch (error) {
        status.innerText = "Kunde inte ansluta till servern.";
    }
}

// 2. Funktion för att hämta data
async function fetchData() {
    output.innerText = "Hämtar...";
    try {
        const response = await fetch('/api/data');

        if (response.status === 401) {
            // Om sessionen löpt ut, tvinga inloggning
            window.location.href = '/oauth2/authorization/keycloak-bff';
            return;
        }

        if (!response.ok) throw new Error(`Fel: ${response.status}`);

        const data = await response.json();
        output.innerText = JSON.stringify(data, null, 2);
    } catch (error) {
        output.innerText = `Fel inträffade: ${error.message}`;
    }
}

// Kör vid sidladdning
initApp();