package ru.example.translation_app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;
import ru.example.translation_app.model.TranslationRequest;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TranslationServiceTest {

    @InjectMocks
    private TranslationService translationService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RestTemplate restTemplate;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        translationService = new TranslationService();
        translationService.setJdbcTemplate(jdbcTemplate);
        translationService.setRestTemplate(restTemplate);
        translationService.setApiKey("demo");
        translationService.setApiUrl("https://apicase.ru/api/translate");
    }

    @Test
    public void testProcessTranslation() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setInputText("red like love");
        request.setOutputLanguage("de");

        String expectedTranslation = "Rot wie Liebe";

        CompletableFuture<String> result = translationService.processTranslation(request);
        String actualTranslation = result.join();

        assertEquals(expectedTranslation, actualTranslation);

        verify(jdbcTemplate, times(1)).update(anyString(), any(Object[].class));
    }

    @Test
    public void testProcessTranslationWrongApiMethod() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setInputText("hello");
        request.setOutputLanguage("fr");

        translationService.setApiUrl("https://apicase.ru/api/errormethod");

        CompletableFuture<String> result = translationService.processTranslation(request);

        assertAll("Проверка, что выбран несуществующий метод запроса к сервису",
                () -> assertThat(result.join(), containsString("404 Not Found")),
                () -> assertThat(result.join(), containsString("Unexpected method")));
    }

    @Test
    public void testProcessTranslationEmptyToken() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setInputText("hello");
        request.setOutputLanguage("fr");

        translationService.setApiKey(null);

        CompletableFuture<String> result = translationService.processTranslation(request);

        assertAll("Проверка, что передан пустой токен",
                () -> assertThat(result.join(), containsString("500 Internal Server Error")),
                () -> assertThat(result.join(), containsString("token is empty"))
        );
    }

    @Test
    public void testProcessTranslationWrongToken() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setInputText("hello");
        request.setOutputLanguage("fr");

        translationService.setApiKey("demo1");

        CompletableFuture<String> result = translationService.processTranslation(request);

        assertThat("Проверка, что передан неверный токен", result.join(), containsString("Wrong token value, please contact us"));
    }

    @Test
    public void testProcessTranslationEmptyText() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setInputText("");
        request.setOutputLanguage("fr");

        CompletableFuture<String> result = translationService.processTranslation(request);

        assertThat("Проверка, что передана пустая строка", result.join(), containsString("text is empty"));
    }

    @Test
    public void testProcessTranslationNoText() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setOutputLanguage("fr");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            translationService.processTranslation(request);
        });

        String expectedMessage = "Input error: text is necessary";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
/*
// Этот тест не запускался, так как у бесплатного токена demo ограниченные возможности перевода, он всегда переводит en->de
    @Test
    public void testProcessTranslationUnsupportedLanguage() {
        TranslationRequest request = new TranslationRequest();
        request.setInputLanguage("en");
        request.setInputText("hello");
        request.setOutputLanguage("thislangisunsup");

        CompletableFuture<String> result = translationService.processTranslation(request);

        assertAll("Проверка, что язык для перевода не поддерживается сервисом",
                () -> assertThat(result.join(), containsString("500 Internal Server Error")),
                () -> assertThat(result.join(), containsString("language is unsupported"))
        );
    }
 */
}