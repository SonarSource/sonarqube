/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.cvss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to load CVSS metadata files packaged within the server JAR.
 */
public class CvssMetadataService {

    private static final String CVSS_METRICS_PREFIX = "cvss-metrics/";
    private static final Logger LOG = LoggerFactory.getLogger(CvssMetadataService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, CvssRuleBreakdown> byRuleKey;

    public CvssMetadataService() {
        this.byRuleKey = loadMetadata();
    }

    public Optional<CvssRuleBreakdown> forRule(String ruleKey) {
        return Optional.ofNullable(byRuleKey.get(ruleKey));
    }

    private Map<String, CvssRuleBreakdown> loadMetadata() {
        Map<String, CvssRuleBreakdown> map = new HashMap<>();
        try {
            File codeSource =
                    new File(CvssMetadataService.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            if (isRunningFromPackagedJar(codeSource)) {
                loadFromJar(map, codeSource);
            }
        } catch (Exception e) {
            LOG.error("Failed to load CVSS metadata: {}", e.getMessage());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Determines whether the server is running from a packaged JAR.
     */
    private static boolean isRunningFromPackagedJar(File codeSource) {
        return codeSource.isFile() && codeSource.getName().endsWith(".jar");
    }

    private void loadFromJar(Map<String, CvssRuleBreakdown> map, File jarFile) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.startsWith(CVSS_METRICS_PREFIX) || !name.endsWith(".json")) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    parseJson(in, map);
                }
            }
        }
    }

    private void parseJson(InputStream in, Map<String, CvssRuleBreakdown> map) throws Exception {
        JsonNode root = mapper.readTree(in);
        // CASE 1: Flat JSON → directly CvssRuleBreakdown
        if (root.has("ruleKey")) {
            CvssRuleBreakdown cvss =
                    mapper.treeToValue(root, CvssRuleBreakdown.class);
            map.put(cvss.getRuleKey(), cvss);
            return;
        }

        // CASE 2: Wrapped JSON → { "Csrf": { ... } }
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            CvssRuleBreakdown cvss =
                    mapper.treeToValue(entry.getValue(), CvssRuleBreakdown.class);
            // Fallback: use wrapper key if ruleKey missing
            if (cvss.getRuleKey() == null) {
                cvss.setRuleKey(entry.getKey());
            }
            map.put(cvss.getRuleKey(), cvss);
        }
    }
}
