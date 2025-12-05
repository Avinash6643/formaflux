package com.formaflux.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class ConversionController {

    private final com.formaflux.service.ConversionService conversionService;

    public ConversionController(com.formaflux.service.ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }

    @PostMapping("/api/analyze")
    @ResponseBody
    public ResponseEntity<?> analyzeFile(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(conversionService.analyzeFile(file));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error analyzing file: " + e.getMessage());
        }
    }

    @PostMapping("/api/convert")
    public ResponseEntity<?> convertFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "mapping", required = false) String mappingJson) {
        try {
            String convertedContent = conversionService.convertFile(file, mappingJson);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"converted.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(convertedContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error converting file: " + e.getMessage());
        }
    }
}
