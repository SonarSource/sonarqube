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
package org.sonar.core.documentation;

import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;

public class VersionedDocumentationBaseLinkProvider implements DocumentationBaseLinkProvider {

  private final String documentationBaseUrl;

  public VersionedDocumentationBaseLinkProvider(String baseUrl, SonarQubeVersion sonarQubeVersion) {
    documentationBaseUrl = completeUrl(baseUrl, sonarQubeVersion.get());
  }

  @Override
  public String getDocumentationBaseUrl() {
    return documentationBaseUrl;
  }

  private static String completeUrl(String baseUrl, Version version) {
    String url = baseUrl;
    if (!url.endsWith("/")) {
      url += "/";
    }
    if ("SNAPSHOT".equals(version.qualifier())) {
      url += "latest";
    } else {
      url += version.major() + "." + version.minor();
    }
    return url;
  }
}
