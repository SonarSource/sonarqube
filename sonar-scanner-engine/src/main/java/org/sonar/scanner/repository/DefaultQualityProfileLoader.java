/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.GetRequest;

import static org.sonar.scanner.util.ScannerUtils.encodeForUrl;

public class DefaultQualityProfileLoader implements QualityProfileLoader {
  private static final String WS_URL = "/api/qualityprofiles/search.protobuf";

  private final Settings settings;
  private final ScannerWsClient wsClient;

  public DefaultQualityProfileLoader(Settings settings, ScannerWsClient wsClient) {
    this.settings = settings;
    this.wsClient = wsClient;
  }

  @Override
  public List<QualityProfile> loadDefault(@Nullable String profileName) {
    StringBuilder url = new StringBuilder(WS_URL + "?defaults=true");
    if (profileName != null) {
      url.append("&profileName=").append(encodeForUrl(profileName));
    }
    getOrganizationKey().ifPresent(k -> url.append("&organization=").append(encodeForUrl(k)));
    return call(url.toString());
  }

  @Override
  public List<QualityProfile> load(String projectKey, @Nullable String profileName) {
    StringBuilder url = new StringBuilder(WS_URL + "?projectKey=").append(encodeForUrl(projectKey));
    if (profileName != null) {
      url.append("&profileName=").append(encodeForUrl(profileName));
    }
    getOrganizationKey().ifPresent(k -> url.append("&organization=").append(encodeForUrl(k)));
    return call(url.toString());
  }

  private Optional<String> getOrganizationKey() {
    return Optional.ofNullable(settings.getString("sonar.organization"));
  }

  private List<QualityProfile> call(String url) {
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
    if (profilesList == null || profilesList.isEmpty()) {
      throw MessageException.of("No quality profiles have been found, you probably don't have any language plugin installed.");
    }
    return profilesList;
  }

}
