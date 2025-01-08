/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.base.Joiner;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;

@ServerSide
public class LogServerVersion implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(LogServerVersion.class);
  private final SonarQubeVersion sonarQubeVersion;

  public LogServerVersion(SonarQubeVersion sonarQubeVersion) {
    this.sonarQubeVersion = sonarQubeVersion;
  }

  @Override
  public void start() {
    String scmRevision = read("/build.properties").getProperty("Implementation-Build");
    Version version = sonarQubeVersion.get();
    LOG.atInfo()
      .addArgument(Joiner.on(" / ").skipNulls().join("Server", version, scmRevision))
      .log("SonarQube {}");
  }

  @Override
  public void stop() {
    // nothing to do
  }

  static Properties read(String filePath) {
    try (InputStream stream = LogServerVersion.class.getResourceAsStream(filePath)) {
      Properties properties = new Properties();
      properties.load(stream);
      return properties;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to read file " + filePath + " from classpath", e);
    }
  }
}
