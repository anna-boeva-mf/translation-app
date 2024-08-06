package ru.example.translation_app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.example.translation_app.model.TranslationRequest;
import ru.example.translation_app.model.TranslationResponse;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class TranslationService {

    @Value("${apicase.translate.api.url}")
    private String apiUrl;

    @Value("${apicase.translate.api.key}")
    private String apiKey;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public CompletableFuture<String> processTranslation(TranslationRequest request) {

        if (request.getInputText() == null) {
            throw new RuntimeException("Input error: text is necessary");
        }

        String[] words = request.getInputText().split(" ");
        List<CompletableFuture<String>> futures = Arrays.stream(words)
                .map(word -> translateWord(word, request.getInputLanguage(), request.getOutputLanguage()))
                .collect(Collectors.toList());
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        CompletableFuture<List<String>> allWordsFuture = allFutures.thenApply(v ->
                futures.stream().map(CompletableFuture::join).collect(Collectors.toList())
        );

        return allWordsFuture.thenApply(translatedWords -> {
            String translatedText = String.join(" ", translatedWords);
            saveTranslationRequest(request, translatedText, null);
            return translatedText;
        }).exceptionally(ex -> {
            saveTranslationRequest(request, null, ex.getMessage());
            return "Error during translation: " + ex.getMessage();
        });
    }

    private CompletableFuture<String> translateWord(String word, String outInLanguage, String outputLanguage) {
        return CompletableFuture.supplyAsync(() -> {
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("token", apiKey)
                    .queryParam("lang_from", outInLanguage)
                    .queryParam("lang_to", outputLanguage)
                    .queryParam("text", word)
                    .toUriString();
            RestTemplate restTemplate = new RestTemplate();
            try {
                String response = restTemplate.getForObject(url, String.class);
                TranslationResponse translationResponse = parseResponse(response);
                if (translationResponse.isTranslated()) {
                    return translationResponse.getText();
                } else {
                    throw new RuntimeException("Translation error: " + translationResponse.getError());
                }
            } catch (Exception e) {
                throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
            }
        }, executorService);
    }

    private TranslationResponse parseResponse(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response, TranslationResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
        }
    }

    private void saveTranslationRequest(TranslationRequest request, String outputText, String errorMsg) {
        String ipAddress = null;
        try {
            ipAddress = String.valueOf(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String sql = "INSERT INTO requests_log (ip_address, input_lang, input_text, output_lang, output_text, error_msg) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, ipAddress, request.getInputLanguage(), request.getInputText(), request.getOutputLanguage(), outputText, errorMsg);
    }

    public void setRestTemplate(RestTemplate restTemplate) {
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}