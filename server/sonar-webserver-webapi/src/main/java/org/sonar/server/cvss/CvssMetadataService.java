package org.sonar.server.cvss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads CVSS metadata from JSON files located under a classpath directory.
 * This implementation relies on classpath resources being available as
 * real filesystem directories (exploded resources).
 */
public class CvssMetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(CvssMetadataService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, CvssScoreBreakdown> metadata;

    public CvssMetadataService() {
        this.metadata = loadMetadata();
    }

    public CvssScoreBreakdown forRule(String ruleKey) {
        return metadata.get(ruleKey);
    }

    public Map<String, CvssScoreBreakdown> getAll() {
        return metadata;
    }

    /**
     * Loads CVSS metadata by resolving the classpath directory to a filesystem path
     * and iterating over JSON files.
     */

    private Map<String, CvssScoreBreakdown> loadMetadata() {
        try {
            URL dirUrl = this.getClass()
                    .getClassLoader()
                    .getResource("cvss-metrics");

            if (dirUrl == null) {
                LOG.warn("CVSS metrics directory not found on classpath");
                return Map.of();
            }

            URI uri = dirUrl.toURI();

            if ("jar".equals(uri.getScheme())) {
                return loadFromJarUri(uri);
            }

        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to load CVSS metadata from filesystem resources", e);
        }
        return Map.of();
    }

    private Map<String, CvssScoreBreakdown> loadFromJarUri(URI jarUri) throws IOException {

        Map<String, CvssScoreBreakdown> map = new HashMap<>();

        // Jar:file:/path/to/jar.jar!/cvss-metrics
        String[] jarParts = jarUri.toString().split("!");
        URI zipUri = URI.create(jarParts[0]); // jar:file:/path/to/jar.jar

        try (FileSystem fileSystem = FileSystems.newFileSystem(zipUri, Map.of())) {
            Path cvssMetricsDir = fileSystem.getPath("/cvss-metrics");

            try (Stream<Path> jsonFilePaths = Files.list(cvssMetricsDir)) {
                jsonFilePaths
                        .filter(p -> p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> {
                            try (InputStream in = Files.newInputStream(p)) {
                                parseJson(in, map);
                            } catch (IOException e) {
                                LOG.error("Failed to parse CVSS file {}", p, e);
                            }
                        });
            }
        }
        return Map.copyOf(map);
    }

    /**
     * Parses a CVSS JSON resource and appends rule entries into the map.
     * Supported formats:
     * 1) Flat JSON with "ruleKey"
     * 2) Wrapped JSON: { "RuleKey": { ... } }
     */
    private void parseJson(InputStream in, Map<String, CvssScoreBreakdown> map) throws IOException {
        JsonNode root = MAPPER.readTree(in);

        // Case 1: flat JSON
        if (root.has("ruleKey")) {
            CvssScoreBreakdown cvss =
                    MAPPER.treeToValue(root, CvssScoreBreakdown.class);

            map.put(cvss.getRuleKey(), cvss);
            return;
        }

        // Case 2: wrapped JSON
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();

            CvssScoreBreakdown cvss =
                    MAPPER.treeToValue(entry.getValue(), CvssScoreBreakdown.class);

            if (cvss.getRuleKey() == null) {
                cvss.setRuleKey(entry.getKey());
            }
            map.put(cvss.getRuleKey(), cvss);
        }
    }
}
