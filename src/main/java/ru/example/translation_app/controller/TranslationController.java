package ru.example.translation_app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.example.translation_app.model.TranslationRequest;
import ru.example.translation_app.service.TranslationService;

import java.util.concurrent.CompletableFuture;

@RestController
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    @PostMapping("/translate")
    public CompletableFuture<String> translateText(@RequestBody TranslationRequest request) {
        return translationService.processTranslation(request);
    }
}