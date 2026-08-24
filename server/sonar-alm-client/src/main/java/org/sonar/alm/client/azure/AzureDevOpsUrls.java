/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.alm.client.azure;

import java.util.Locale;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;

public final class AzureDevOpsUrls {

  private static final String CLOUD_HOST = "dev.azure.com";
  private static final String CLOUD_HOST_SUFFIX = ".visualstudio.com";

  private AzureDevOpsUrls() {
    // utility class
  }

  /**
   * True when {@code url} points at Azure DevOps Services (the SaaS product: {@code dev.azure.com}
   * or the legacy {@code *.visualstudio.com} hostnames), as opposed to Azure DevOps Server (on-prem/TFS).
   * A null or unparseable URL is treated as Server, i.e. not Azure DevOps Services.
   */
  public static boolean isAzureDevOpsServices(@Nullable String url) {
    if (url == null) {
      return false;
    }
    HttpUrl parsed = HttpUrl.parse(url);
    if (parsed == null) {
      return false;
    }
    String host = parsed.host().toLowerCase(Locale.ROOT);
    return CLOUD_HOST.equals(host) || host.endsWith(CLOUD_HOST_SUFFIX);
  }
}
