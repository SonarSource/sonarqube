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
package org.sonar.scanner.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.scan.ScanProperties;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.scanner.util.ScannerUtils.encodeForUrl;

public class DefaultQualityProfileLoader implements QualityProfileLoader {
  private static final String WS_URL = "/api/qualityprofiles/search.protobuf";

  private final ScannerWsClient wsClient;
  private final ScanProperties properties;

  public DefaultQualityProfileLoader(ScanProperties properties, ScannerWsClient wsClient) {
    this.properties = properties;
    this.wsClient = wsClient;
  }

  @Override
  public List<QualityProfile> loadDefault() {
    StringBuilder url = new StringBuilder(WS_URL + "?defaults=true");
    return handleErrors(url, () -> "Failed to load the default quality profiles");
  }

  @Override
  public List<QualityProfile> load(String projectKey) {
    StringBuilder url = new StringBuilder(WS_URL + "?projectKey=").append(encodeForUrl(projectKey));
    return handleErrors(url, () -> String.format("Failed to load the quality profiles of project '%s'", projectKey));
  }

  private List<QualityProfile> handleErrors(StringBuilder url, Supplier<String> errorMsg) {
    try {
      return doLoad(url);
    } catch (HttpException e) {
      if (e.code() == 404) {
        throw MessageException.of(errorMsg.get() + ": " + ScannerWsClient.createErrorMessage(e));
      }
      throw new IllegalStateException(errorMsg.get() + ": " + ScannerWsClient.createErrorMessage(e));
    } catch (MessageException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(errorMsg.get(), e);
    }
  }

  private List<QualityProfile> doLoad(StringBuilder url) throws IOException {
    properties.organizationKey().ifPresent(k -> url.append("&organization=").append(encodeForUrl(k)));
    Map<String, QualityProfile> result = call(url.toString());

    if (result.isEmpty()) {
      throw MessageException.of("No quality profiles have been found, you probably don't have any language plugin installed.");
    }

    return new ArrayList<>(result.values());
  }

  private Map<String, QualityProfile> call(String url) throws IOException {
    GetRequest getRequest = new GetRequest(url);
    try (InputStream is = wsClient.call(getRequest).contentStream()) {
      SearchWsResponse profiles = SearchWsResponse.parseFrom(is);
      List<QualityProfile> profilesList = profiles.getProfilesList();
      return profilesList.stream().collect(toMap(QualityProfile::getLanguage, identity(), throwingMerger(), LinkedHashMap::new));
    }
  }

  private static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }

}
