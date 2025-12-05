package com.formaflux.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.formaflux.model.FileStructure;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class ConversionService {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public FileStructure analyzeFile(MultipartFile file) throws IOException {
        JsonNode node = xmlMapper.readTree(file.getInputStream());
        List<String> fields = new ArrayList<>();
        extractFields(node, "", fields);

        FileStructure structure = new FileStructure();
        structure.setFileName(file.getOriginalFilename());
        structure.setFields(fields);
        return structure;
    }

    public String convertFile(MultipartFile file, String mappingJson) throws IOException {
        JsonNode node = xmlMapper.readTree(file.getInputStream());

        if (mappingJson == null || mappingJson.isEmpty()) {
            // Normal conversion
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } else {
            // Advanced conversion
            Map<String, String> mapping = jsonMapper.readValue(mappingJson, Map.class);
            com.fasterxml.jackson.databind.node.ObjectNode resultNode = jsonMapper.createObjectNode();

            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                String sourcePath = entry.getKey();
                String targetKey = entry.getValue();

                JsonNode valueNode = getNodeAt(node, sourcePath);
                if (valueNode != null && !valueNode.isMissingNode()) {
                    resultNode.set(targetKey, valueNode);
                }
            }
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultNode);
        }
    }

    private JsonNode getNodeAt(JsonNode root, String path) {
        if (path == null || path.isEmpty())
            return root;
        String[] parts = path.split("\\.");
        JsonNode current = root;
        for (String part : parts) {
            if (part.contains("[")) {
                String name = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                current = current.get(name).get(index);
            } else {
                current = current.get(part);
            }
            if (current == null)
                return null;
        }
        return current;
    }

    private void extractFields(JsonNode node, String prefix, List<String> fields) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                String currentPath = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                fields.add(currentPath);
                extractFields(field.getValue(), currentPath, fields);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                extractFields(node.get(i), prefix + "[" + i + "]", fields);
            }
        }
    }
}
