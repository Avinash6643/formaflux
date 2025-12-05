package com.formaflux.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formaflux.model.FileStructure;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConversionServiceTest {

    private final ConversionService conversionService = new ConversionService();

    @Test
    void testAnalyzeFile() throws IOException {
        String xmlContent = "<root><name>John</name><age>30</age></root>";
        MockMultipartFile file = new MockMultipartFile("file", "test.xml", "text/xml",
                xmlContent.getBytes(StandardCharsets.UTF_8));

        FileStructure structure = conversionService.analyzeFile(file);

        assertEquals("test.xml", structure.getFileName());
        assertTrue(structure.getFields().contains("name"));
        assertTrue(structure.getFields().contains("age"));
    }

    @Test
    void testNormalConversion() throws IOException {
        String xmlContent = "<root><name>John</name></root>";
        MockMultipartFile file = new MockMultipartFile("file", "test.xml", "text/xml",
                xmlContent.getBytes(StandardCharsets.UTF_8));

        String json = conversionService.convertFile(file, null);

        assertTrue(json.contains("\"name\" : \"John\""));
    }

    @Test
    void testAdvancedConversion() throws IOException {
        String xmlContent = "<root><user><name>John</name><details><age>30</age></details></user></root>";
        MockMultipartFile file = new MockMultipartFile("file", "test.xml", "text/xml",
                xmlContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> mapping = new HashMap<>();
        mapping.put("user.name", "userName");
        mapping.put("user.details.age", "userAge");
        String mappingJson = new ObjectMapper().writeValueAsString(mapping);

        String json = conversionService.convertFile(file, mappingJson);

        assertTrue(json.contains("\"userName\" : \"John\""));
        assertTrue(json.contains("\"userAge\" : \"30\""));
    }

    @Test
    void testStructuralTransformation() throws IOException {
        String xmlContent = "<library><book id=\"bk101\"><title>Hitchhiker</title><author>Adams</author><genre>SciFi</genre></book>"
                +
                "<book id=\"bk102\"><title>1984</title><author>Orwell</author><genre>Dystopian</genre></book></library>";
        MockMultipartFile file = new MockMultipartFile("file", "test.xml", "text/xml",
                xmlContent.getBytes(StandardCharsets.UTF_8));

        Map<String, String> mapping = new HashMap<>();
        mapping.put("book[].title", "book[].bookdetails.title");
        mapping.put("book[].author", "book[].bookdetails.author");
        String mappingJson = new ObjectMapper().writeValueAsString(mapping);

        String json = conversionService.convertFile(file, mappingJson);

        assertTrue(json.contains("\"bookdetails\" : {"));
        assertTrue(json.contains("\"title\" : \"Hitchhiker\""));
        assertTrue(json.contains("\"author\" : \"Adams\""));
    }
}
