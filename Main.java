import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Main {
    private static final String GEMINI_API_KEY; //= "AIzaSyDCaFFGV_5f_wu-deQkqgGfqCvLOcoohPc"; // Replace with your actual key
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/recommend", new RecommendationHandler());
        server.start();
        System.out.println("Backend running on http://localhost:8080");
    }

    static class RecommendationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Set CORS headers
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {
                
                // Read and parse request
                String requestBody = br.lines().collect(Collectors.joining("\n"));
                String price = extractJsonValue(requestBody, "price");
                String usage = extractJsonValue(requestBody, "usage");

                if (price.isEmpty() || usage.isEmpty()) {
                    throw new Exception("Missing price or usage in request");
                }

                // Call Gemini API and get raw JSON response
                String geminiResponse = callGeminiAPI(price, usage);
                
                // Create response object with the raw JSON
                String response = "{\"recommendation\":" + geminiResponse + "}";
                
                // Send response
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                // Error handling
                String error = "{\"error\":\"" + e.getMessage()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"") + "\"}";
                exchange.sendResponseHeaders(500, error.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes(StandardCharsets.UTF_8));
                }
                System.err.println("Error processing request: " + e.getMessage());
            }
        }

        private String extractJsonValue(String json, String key) {
            int keyIndex = json.indexOf("\"" + key + "\":");
            if (keyIndex == -1) return "";
            
            int valueStart = json.indexOf(":", keyIndex) + 1;
            int quoteStart = json.indexOf("\"", valueStart);
            if (quoteStart == -1) return "";
            
            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            if (quoteEnd == -1) return "";
            
            return json.substring(quoteStart + 1, quoteEnd);
        }

        private String callGeminiAPI(String price, String usage) throws Exception {
            // Create the prompt with clear instructions for JSON response
            String prompt = "Suggest  laptops under ₹" + price + " for " + usage + ". " +
                         "Return as a valid JSON array with these fields for each: " +
                         "brand, model, price, and key_specs. " +
                         "Example format: " +
                         "[{\"brand\":\"Brand1\",\"model\":\"ModelX\",\"price\":\"₹45,000\",\"key_specs\":\"Spec1, Spec2\"}]";

            // Build the request JSON
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + 
                               prompt.replace("\\", "\\\\")
                                    .replace("\"", "\\\"") + 
                               "\"}]}]}";

            // Make the API call
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Validate response
            if (response.statusCode() != 200) {
                throw new Exception("API returned status: " + response.statusCode());
            }

            return response.body();
        }
    }
}
