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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.Version;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.platform.SonarQubeVersion;

public class DefaultDocumentationLinkGenerator implements DocumentationLinkGenerator {
  public static final String DOCUMENTATION_PUBLIC_URL = "https://docs.sonarsource.com/sonarqube/";

  private final String documentationBaseUrl;

  public DefaultDocumentationLinkGenerator(SonarQubeVersion sonarQubeVersion, Configuration configuration) {
    this.documentationBaseUrl = completeUrl(configuration.get(CorePropertyDefinitions.DOCUMENTATION_BASE_URL)
      .orElse(DOCUMENTATION_PUBLIC_URL), sonarQubeVersion.get());
  }

  private static String completeUrl(String baseUrl, Version version) {
    String url = baseUrl;
    if (!url.endsWith("/")) {
      url += "/";
    }
    if (version.qualifier().equals("SNAPSHOT")) {
      url += "latest";
    } else {
      url += version.major() + "." + version.minor();
    }
    return url;
  }

  @Override
  public String getDocumentationLink(@Nullable String suffix) {
    return documentationBaseUrl + Optional.ofNullable(suffix).orElse("");
  }
}
