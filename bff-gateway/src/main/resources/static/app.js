document.getElementById('fetchBtn').addEventListener('click', async () => {
    const outputElement = document.getElementById('output');
    outputElement.innerText = "Hämtar...";

    try {
        const response = await fetch('/api/data');

        if (response.status === 401 || response.redirected) {
            outputElement.innerText = "Session saknas eller har löpt ut. Omdirigerar till Keycloak...";

            // Ändra från window.location.reload() till detta:
            window.location.href = '/oauth2/authorization/keycloak-bff';
            return;
        }

        if (!response.ok) {
            throw new Error(`Serverfel: ${response.status}`);
        }

        const data = await response.json();
        outputElement.innerText = JSON.stringify(data, null, 2);

    } catch (error) {
        outputElement.innerText = `Fel inträffade: ${error.message}`;
    }
});
