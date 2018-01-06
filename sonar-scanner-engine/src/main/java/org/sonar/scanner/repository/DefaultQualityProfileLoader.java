/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.GetRequest;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.core.config.ScannerProperties.ORGANIZATION;
import static org.sonar.scanner.util.ScannerUtils.encodeForUrl;

public class DefaultQualityProfileLoader implements QualityProfileLoader {
  private static final String WS_URL = "/api/qualityprofiles/search.protobuf";

  private final Configuration settings;
  private final ScannerWsClient wsClient;

  public DefaultQualityProfileLoader(Configuration settings, ScannerWsClient wsClient) {
    this.settings = settings;
    this.wsClient = wsClient;
  }

  @Override
  public List<QualityProfile> loadDefault(@Nullable String profileName) {
    StringBuilder url = new StringBuilder(WS_URL + "?defaults=true");
    return loadAndOverrideIfNeeded(profileName, url);
  }

  @Override
  public List<QualityProfile> load(String projectKey, @Nullable String profileName) {
    StringBuilder url = new StringBuilder(WS_URL + "?projectKey=").append(encodeForUrl(projectKey));
    return loadAndOverrideIfNeeded(profileName, url);
  }

  private List<QualityProfile> loadAndOverrideIfNeeded(@Nullable String profileName, StringBuilder url) {
    getOrganizationKey().ifPresent(k -> url.append("&organization=").append(encodeForUrl(k)));
    Map<String, QualityProfile> result = call(url.toString());

    if (profileName != null) {
      StringBuilder urlForName = new StringBuilder(WS_URL + "?profileName=");
      urlForName.append(encodeForUrl(profileName));
      getOrganizationKey().ifPresent(k -> urlForName.append("&organization=").append(encodeForUrl(k)));
      result.putAll(call(urlForName.toString()));
    }
    if (result.isEmpty()) {
      throw MessageException.of("No quality profiles have been found, you probably don't have any language plugin installed.");
    }

    return new ArrayList<>(result.values());
  }

  private Optional<String> getOrganizationKey() {
    return settings.get(ORGANIZATION);
  }

  private Map<String, QualityProfile> call(String url) {
    GetRequest getRequest = new GetRequest(url);
    InputStream is = wsClient.call(getRequest).contentStream();
    SearchWsResponse profiles;

    try {
      profiles = SearchWsResponse.parseFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load quality profiles", e);
    } finally {
      IOUtils.closeQuietly(is);
    }

    List<QualityProfile> profilesList = profiles.getProfilesList();
    return profilesList.stream()
      .collect(toMap(QualityProfile::getLanguage, identity(), throwingMerger(), LinkedHashMap::new));
  }

  private static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }

}
