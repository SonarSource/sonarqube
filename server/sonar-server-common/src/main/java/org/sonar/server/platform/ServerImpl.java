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
package org.sonar.server.platform;

import java.io.File;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;

@ComputeEngineSide
@ServerSide
public class ServerImpl extends Server {

  private final Configuration config;
  private final StartupMetadata state;
  private final ServerFileSystem fs;
  private final UrlSettings urlSettings;
  private final SonarRuntime runtime;

  public ServerImpl(Configuration config, StartupMetadata state, ServerFileSystem fs, UrlSettings urlSettings, SonarRuntime runtime) {
    this.config = config;
    this.state = state;
    this.fs = fs;
    this.urlSettings = urlSettings;
    this.runtime = runtime;
  }

  /**
   * Can be null when server is waiting for migration
   */
  @Override
  @CheckForNull
  public String getId() {
    return config.get(CoreProperties.SERVER_ID).orElse(null);
  }

  @Override
  public String getPermanentServerId() {
    return getId();
  }

  @Override
  public String getVersion() {
    return runtime.getApiVersion().toString();
  }

  @Override
  public Date getStartedAt() {
    return new Date(state.getStartedAt());
  }

  @Override
  public File getRootDir() {
    return fs.getHomeDir();
  }
  
  @Override
  public String getContextPath() {
    return urlSettings.getContextPath();
  }

  @Override
  public String getPublicRootUrl() {
    return urlSettings.getBaseUrl();
  }

  @Override
  public boolean isDev() {
    return false;
  }

  @Override
  public boolean isSecured() {
    return urlSettings.isSecured();
  }

  @Override
  public String getURL() {
    return urlSettings.getBaseUrl();
  }

}
