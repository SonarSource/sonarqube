/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.BatchWsClient;
import org.sonar.scanner.util.BatchUtils;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.apache.commons.io.IOUtils;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.GetRequest;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DefaultQualityProfileLoader implements QualityProfileLoader {
  private static final String WS_URL = "/api/qualityprofiles/search.protobuf";

  private BatchWsClient wsClient;

  public DefaultQualityProfileLoader(BatchWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public List<QualityProfile> loadDefault(@Nullable String profileName) {
    String url = WS_URL + "?defaults=true";
    if (profileName != null) {
      url += "&profileName=" + BatchUtils.encodeForUrl(profileName);
    }
    return loadResource(url);
  }

  @Override
  public List<QualityProfile> load(String projectKey, @Nullable String profileName) {
    String url = WS_URL + "?projectKey=" + BatchUtils.encodeForUrl(projectKey);
    if (profileName != null) {
      url += "&profileName=" + BatchUtils.encodeForUrl(profileName);
    }
    return loadResource(url);
  }

  private List<QualityProfile> loadResource(String url) {
    GetRequest getRequest = new GetRequest(url);
    InputStream is = wsClient.call(getRequest).contentStream();
    SearchWsResponse profiles = null;

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
