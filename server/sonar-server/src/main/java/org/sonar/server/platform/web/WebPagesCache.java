/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.web;

import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Configuration;
import org.sonar.server.platform.OfficialDistribution;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.Platform.Status;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;
import static org.sonar.server.platform.Platform.Status.UP;

public class WebPagesCache {

  private static final String WEB_CONTEXT_PLACEHOLDER = "%WEB_CONTEXT%";
  private static final String SERVER_STATUS_PLACEHOLDER = "%SERVER_STATUS%";
  private static final String INSTANCE_PLACEHOLDER = "%INSTANCE%";
  private static final String OFFICIAL_PLACEHOLDER = "%OFFICIAL%";

  private static final String SONARCLOUD_INSTANCE_VALUE = "SonarCloud";
  private static final String SONARQUBE_INSTANCE_VALUE = "SonarQube";

  private static final String INDEX_HTML_PATH = "/index.html";

  private static final Set<String> HTML_PATHS = ImmutableSet.of(INDEX_HTML_PATH, "/integration/vsts/index.html");

  private final Platform platform;
  private final Configuration configuration;
  private final OfficialDistribution officialDistribution;

  private ServletContext servletContext;
  private Map<String, String> indexHtmlByPath;
  private Status status;

  public WebPagesCache(Platform platform, Configuration configuration, OfficialDistribution officialDistribution) {
    this.platform = platform;
    this.configuration = configuration;
    this.indexHtmlByPath = new HashMap<>();
    this.officialDistribution = officialDistribution;
  }

  public void init(ServletContext servletContext) {
    this.servletContext = servletContext;
    generate(platform.status());
  }

  public String getContent(String path) {
    String htmlPath = HTML_PATHS.contains(path) ? path : INDEX_HTML_PATH;
    checkState(servletContext != null, "init has not been called");
    // Optimization to not have to call platform.currentStatus on each call
    if (Objects.equals(status, UP)) {
      return indexHtmlByPath.get(htmlPath);
    }
    Status currentStatus = platform.status();
    if (!Objects.equals(status, currentStatus)) {
      generate(currentStatus);
    }
    return indexHtmlByPath.get(htmlPath);
  }

  private void generate(Status status) {
    this.status = status;
    HTML_PATHS.forEach(path -> indexHtmlByPath.put(path, provide(path)));
  }

  private String provide(String path) {
    getClass().getResourceAsStream(INDEX_HTML_PATH);
    boolean isSonarCloud = configuration.getBoolean(SONARCLOUD_ENABLED.getKey()).orElse(false);
    String instance = isSonarCloud ? SONARCLOUD_INSTANCE_VALUE : SONARQUBE_INSTANCE_VALUE;
    return loadHtmlFile(path, status.name(), instance);
  }

  private String loadHtmlFile(String path, String serverStatus, String instance) {
    try (InputStream input = servletContext.getResourceAsStream(path)) {
      String template = IOUtils.toString(requireNonNull(input), UTF_8);
      return template
        .replaceAll(WEB_CONTEXT_PLACEHOLDER, servletContext.getContextPath())
        .replaceAll(SERVER_STATUS_PLACEHOLDER, serverStatus)
        .replaceAll(INSTANCE_PLACEHOLDER, instance)
        .replaceAll(OFFICIAL_PLACEHOLDER, String.valueOf(officialDistribution.check()));
    } catch (Exception e) {
      throw new IllegalStateException("Fail to load file " + path, e);
    }
  }
}
