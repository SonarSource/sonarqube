/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.repository;

import org.sonarqube.ws.QualityProfiles.WsSearchResponse;

import org.sonar.batch.util.BatchUtils;
import org.apache.commons.io.IOUtils;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class DefaultQualityProfileLoader implements QualityProfileLoader {
  private static final String WS_URL = "/qualityprofiles/search";

  private WSLoader wsLoader;

  public DefaultQualityProfileLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public List<QualityProfile> loadDefault(@Nullable MutableBoolean fromCache) {
    String url = WS_URL + "?defaults=true";
    return loadResource(url, fromCache);
  }

  @Override
  public List<QualityProfile> load(String projectKey, @Nullable String profileName, @Nullable MutableBoolean fromCache) {
    String url = WS_URL + "?projectKey=" + BatchUtils.encodeForUrl(projectKey);
    if (profileName != null) {
      url += "&profileName=" + BatchUtils.encodeForUrl(profileName);
    }
    return loadResource(url, fromCache);
  }

  private static void validate(Collection<QualityProfile> profiles) {
    if (profiles == null || profiles.isEmpty()) {
      throw new IllegalStateException("No quality profiles has been found this project, you probably don't have any language plugin suitable for this analysis.");
    }
  }

  private List<QualityProfile> loadResource(String url, @Nullable MutableBoolean fromCache) {
    WSLoaderResult<InputStream> result = wsLoader.loadStream(url);
    if (fromCache != null) {
      fromCache.setValue(result.isFromCache());
    }
    InputStream is = result.get();
    WsSearchResponse profiles = null;

    try {
      profiles = WsSearchResponse.parseFrom(is);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load quality profiles", e);
    } finally {
      IOUtils.closeQuietly(is);
    }

    List<QualityProfile> profilesList = profiles.getProfilesList();
    validate(profilesList);
    return profilesList;
  }

}
