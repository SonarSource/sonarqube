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
package org.sonar.scanner.platform;

import java.io.File;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.DateUtils;
import org.sonar.scanner.bootstrap.ScannerWsClient;

import static org.apache.commons.lang.StringUtils.trimToEmpty;

public class DefaultServer extends Server {

  private final Configuration settings;
  private final ScannerWsClient client;
  private final SonarRuntime runtime;

  public DefaultServer(Configuration settings, ScannerWsClient client, SonarRuntime runtime) {
    this.settings = settings;
    this.client = client;
    this.runtime = runtime;
  }

  @Override
  public String getId() {
    return settings.get(CoreProperties.SERVER_ID).orElseThrow(() -> new IllegalStateException("Mandatory"));
  }

  @Override
  public String getVersion() {
    return runtime.getApiVersion().toString();
  }

  @Override
  public Date getStartedAt() {
    String dateString = settings.get(CoreProperties.SERVER_STARTTIME).orElseThrow(() -> new IllegalStateException("Mandatory"));
    return DateUtils.parseDateTime(dateString);
  }

  @Override
  public File getRootDir() {
    return null;
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public String getPublicRootUrl() {
    String baseUrl = trimToEmpty(settings.get(CoreProperties.SERVER_BASE_URL).orElse(""));
    if (baseUrl.isEmpty()) {
      // If server base URL was not configured in Sonar server then is is better to take URL configured on batch side
      baseUrl = client.baseUrl();
    }
    return StringUtils.removeEnd(baseUrl, "/");
  }

  @Override
  public boolean isDev() {
    return false;
  }

  @Override
  public boolean isSecured() {
    return false;
  }

  @Override
  public String getURL() {
    return StringUtils.removeEnd(client.baseUrl(), "/");
  }

  @Override
  public String getPermanentServerId() {
    return getId();
  }
}
