package org.dromara.carbon.enterprise.shared.option.service.impl;

import org.dromara.common.core.utils.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GB/T 4754-2017 company industry hierarchy catalog.
 */
final class CeCompanyIndustryCatalog {

    private static final String RESOURCE = "enterprise-options/gbt4754-2017-industry.csv";

    private CeCompanyIndustryCatalog() {
    }

    static List<Map<String, String>> rows() {
        List<Map<String, String>> rows = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        if (!resource.exists()) {
            return rows;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> headers = parseCsvLine(reader.readLine());
            if (headers.isEmpty()) {
                return rows;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                append(rows, headers, line);
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return rows;
    }

    private static void append(List<Map<String, String>> rows, List<String> headers, String line) {
        if (StringUtils.isBlank(line)) {
            return;
        }
        List<String> values = parseCsvLine(line);
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), i < values.size() ? values.get(i) : "");
        }
        rows.add(row);
    }

    private static List<String> parseCsvLine(String line) {
        if (StringUtils.isBlank(line)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
