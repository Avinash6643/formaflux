package com.formaflux.model;

import lombok.Data;
import java.util.List;

@Data
public class FileStructure {
    private String fileName;
    private List<String> fields;
}
