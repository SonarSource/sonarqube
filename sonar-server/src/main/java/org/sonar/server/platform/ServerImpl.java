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
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public final class ServerImpl extends Server implements Startable {
  private static final Logger LOG = LoggerFactory.getLogger(ServerImpl.class);

  private final Settings settings;
  private final Date startedAt;
  private final String buildProperties;
  private final String pomProperties;
  private String id;
  private String version;
  private String implementationBuild;

  public ServerImpl(Settings settings) {
    this(settings, "/build.properties", "/META-INF/maven/org.codehaus.sonar/sonar-plugin-api/pom.properties");
  }

  @VisibleForTesting
  ServerImpl(Settings settings, String buildProperties, String pomProperties) {
    this.settings = settings;
    this.startedAt = new Date();
    this.buildProperties = buildProperties;
    this.pomProperties = pomProperties;
  }

  @Override
  public void start() {
    try {
      id = new SimpleDateFormat("yyyyMMddHHmmss").format(startedAt);

      version = read(pomProperties).getProperty("version", "");
      implementationBuild = read(buildProperties).getProperty("Implementation-Build");

      if (StringUtils.isBlank(version)) {
        throw new IllegalStateException("Unknown SonarQube version");
      }

      LOG.info("SonarQube {}", Joiner.on(" / ").skipNulls().join("Server", version, implementationBuild));

    } catch (IOException e) {
      throw new IllegalStateException("Can not load metadata", e);
    }
  }

  @Override
  public void stop() {
    // do nothing
  }

  @Override
  public String getPermanentServerId() {
    return settings.getString(CoreProperties.PERMANENT_SERVER_ID);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public String getImplementationBuild() {
    return implementationBuild;
  }

  @Override
  public Date getStartedAt() {
    return startedAt;
  }

  private static Properties read(String filename) throws IOException {
    Properties properties = new Properties();

    InputStream stream = null;
    try {
      stream = ServerImpl.class.getResourceAsStream(filename);
      if (stream != null) {
        properties.load(stream);
      }
    } finally {
      IOUtils.closeQuietly(stream);
    }

    return properties;
  }
  @Override
  public String getURL() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServerImpl other = (ServerImpl) o;
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
