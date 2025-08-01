package org.arkadipta.opus.service;

import org.arkadipta.opus.dto.QuoteResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class QuoteService {

    @Autowired
    private RestTemplate restTemplate; // ✅ Inject instead of creating

    @Value("${api.ninja.key:your-default-api-key}")
    private String apiKey;

    private final String API_URL = "https://api.api-ninjas.com/v1/quotes";

    public QuoteResponse getRandomQuote() {
        try {
            // Set up headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", apiKey);

            // Create HTTP entity with headers
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make the API call using injected RestTemplate
            ResponseEntity<QuoteResponse[]> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.GET,
                    entity,
                    QuoteResponse[].class);

            // Extract the first quote from the array response
            QuoteResponse[] quotes = response.getBody();
            if (quotes != null && quotes.length > 0) {
                return quotes[0];
            }

            // Return default quote if API fails
            return new QuoteResponse("Health check successful!", "Opus API");

        } catch (Exception e) {
            // Fallback quote in case of API failure
            return new QuoteResponse("Health check successful - API is running!", "Opus API");
        }
    }
}