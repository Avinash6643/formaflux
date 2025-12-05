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
        java.util.Set<String> fields = new java.util.LinkedHashSet<>();
        extractGenericFields(node, "", fields);

        FileStructure structure = new FileStructure();
        structure.setFileName(file.getOriginalFilename());
        structure.setFields(new ArrayList<>(fields));
        return structure;
    }

    public String convertFile(MultipartFile file, String mappingJson) throws IOException {
        JsonNode node = xmlMapper.readTree(file.getInputStream());

        if (mappingJson == null || mappingJson.isEmpty()) {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } else {
            Map<String, String> mapping = jsonMapper.readValue(mappingJson, Map.class);
            Map<String, JsonNode> flattened = new java.util.LinkedHashMap<>();
            flatten(node, "", flattened);

            Map<String, JsonNode> remapped = new java.util.LinkedHashMap<>();

            for (Map.Entry<String, JsonNode> entry : flattened.entrySet()) {
                String sourcePath = entry.getKey();
                String genericPath = sourcePath.replaceAll("\\[\\d+\\]", "[]");

                if (mapping.containsKey(genericPath)) {
                    String targetGenericPath = mapping.get(genericPath);
                    if (targetGenericPath != null && !targetGenericPath.isEmpty()) {
                        String targetPath = injectIndices(sourcePath, targetGenericPath);
                        remapped.put(targetPath, entry.getValue());
                    }
                } else {
                    // Keep original if not mapped (optional, but good for partial mapping)
                    // remapped.put(sourcePath, entry.getValue());
                }
            }

            JsonNode resultNode = unflatten(remapped);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultNode);
        }
    }

    private void extractGenericFields(JsonNode node, String prefix, java.util.Set<String> fields) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                String currentPath = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                if (field.getValue().isValueNode()) {
                    fields.add(currentPath);
                } else {
                    extractGenericFields(field.getValue(), currentPath, fields);
                }
            }
        } else if (node.isArray()) {
            if (node.size() > 0) {
                // Process only the first element to get the schema
                extractGenericFields(node.get(0), prefix + "[]", fields);
            }
        } else {
            if (!prefix.isEmpty())
                fields.add(prefix);
        }
    }

    private void flatten(JsonNode node, String prefix, Map<String, JsonNode> flattened) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                String currentPath = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                flatten(field.getValue(), currentPath, flattened);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flatten(node.get(i), prefix + "[" + i + "]", flattened);
            }
        } else {
            flattened.put(prefix, node);
        }
    }

    private JsonNode unflatten(Map<String, JsonNode> flattened) {
        com.fasterxml.jackson.databind.node.ObjectNode root = jsonMapper.createObjectNode();

        for (Map.Entry<String, JsonNode> entry : flattened.entrySet()) {
            String path = entry.getKey();
            JsonNode value = entry.getValue();
            String[] parts = path.split("\\.");

            JsonNode current = root;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String name = part;
                Integer index = null;

                if (part.contains("[")) {
                    name = part.substring(0, part.indexOf("["));
                    index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                }

                if (i == parts.length - 1) {
                    // Leaf node
                    if (index != null) {
                        ensureArray(current, name).set(index, value);
                    } else {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) current).set(name, value);
                    }
                } else {
                    // Intermediate node
                    if (index != null) {
                        current = ensureObjectInArray(current, name, index);
                    } else {
                        current = ensureObject(current, name);
                    }
                }
            }
        }
        return root;
    }

    private com.fasterxml.jackson.databind.node.ArrayNode ensureArray(JsonNode parent, String name) {
        if (!parent.has(name) || !parent.get(name).isArray()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) parent).set(name, jsonMapper.createArrayNode());
        }
        return (com.fasterxml.jackson.databind.node.ArrayNode) parent.get(name);
    }

    private JsonNode ensureObject(JsonNode parent, String name) {
        if (!parent.has(name) || !parent.get(name).isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) parent).set(name, jsonMapper.createObjectNode());
        }
        return parent.get(name);
    }

    private JsonNode ensureObjectInArray(JsonNode parent, String name, int index) {
        com.fasterxml.jackson.databind.node.ArrayNode array = ensureArray(parent, name);
        while (array.size() <= index) {
            array.add(jsonMapper.createObjectNode());
        }
        if (!array.get(index).isObject()) {
            array.set(index, jsonMapper.createObjectNode());
        }
        return array.get(index);
    }

    private String injectIndices(String sourcePath, String targetGenericPath) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[(\\d+)\\]").matcher(sourcePath);
        StringBuilder result = new StringBuilder();

        // Find all indices in source path
        List<String> indices = new ArrayList<>();
        while (matcher.find()) {
            indices.add(matcher.group(1));
        }

        // Inject into target path
        int indexCount = 0;
        for (int i = 0; i < targetGenericPath.length(); i++) {
            if (targetGenericPath.startsWith("[]", i)) {
                if (indexCount < indices.size()) {
                    result.append("[").append(indices.get(indexCount++)).append("]");
                    i++; // Skip the next ']'
                } else {
                    result.append("[]"); // Should not happen if structure matches
                }
            } else {
                result.append(targetGenericPath.charAt(i));
            }
        }
        return result.toString();
    }
}
