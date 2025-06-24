async function getLaptopRecommendation() {
    const price = document.getElementById("price").value;
    const usage = document.getElementById("usage").value;
    const responseDiv = document.getElementById("response");

    if (!price || !usage) {
        alert("Please enter both budget and usage!");
        return;
    }

    try {
        const response = await fetch("http://localhost:8080/recommend", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ price, usage }),
        });

        if (!response.ok) throw new Error("API request failed");

        const data = await response.json();
        responseDiv.innerHTML = `<strong>Gemini Recommendation:</strong><br>${data.recommendation}`;
    } catch (error) {
        responseDiv.innerHTML = `Error: ${error.message}`;
    }
}