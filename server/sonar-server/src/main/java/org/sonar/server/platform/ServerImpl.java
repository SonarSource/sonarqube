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
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.annotation.CheckForNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.server.app.TomcatContexts;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.api.CoreProperties.SERVER_BASE_URL;
import static org.sonar.api.CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE;
import static org.sonar.server.app.TomcatContexts.PROPERTY_CONTEXT;

public final class ServerImpl extends Server implements Startable {
  private static final String PROPERTY_SONAR_CORE_STARTED_AT = "sonar.core.startedAt";

  private static final Logger LOG = Loggers.get(ServerImpl.class);

  private final Settings settings;
  private final String buildProperties;
  private final String versionPath;
  private Date startedAt;
  private String id;
  private String version;
  private String implementationBuild;
  private File sonarHome;
  private File deployDir;
  private String contextPath;

  public ServerImpl(Settings settings) {
    this(settings, "/build.properties", "/sq-version.txt");
  }

  @VisibleForTesting
  ServerImpl(Settings settings, String buildProperties, String versionPath) {
    this.settings = settings;
    this.buildProperties = buildProperties;
    this.versionPath = versionPath;
  }

  @Override
  public void start() {
    try {
      String startedAtString = settings.getString(PROPERTY_SONAR_CORE_STARTED_AT);
      checkState(startedAtString != null, "property %s must be set", PROPERTY_SONAR_CORE_STARTED_AT);
      startedAt = new Date(Long.valueOf(startedAtString));
      id = new SimpleDateFormat("yyyyMMddHHmmss").format(startedAt);

      version = readVersion(versionPath);
      implementationBuild = read(buildProperties).getProperty("Implementation-Build");
      sonarHome = new File(settings.getString(ProcessProperties.PATH_HOME));
      if (!sonarHome.isDirectory()) {
        throw new IllegalStateException("SonarQube home directory is not valid");
      }

      deployDir = new File(settings.getString(ProcessProperties.PATH_DATA), TomcatContexts.WEB_DEPLOY_PATH_RELATIVE_TO_DATA_DIR);

      contextPath = StringUtils.defaultIfBlank(settings.getString(PROPERTY_CONTEXT), "")
        // Remove trailing slashes
        .replaceFirst("(\\/+)$", "");

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
    return checkNotNull(startedAt, "start() method has not been called");
  }

  @Override
  public File getRootDir() {
    return sonarHome;
  }

  @Override
  @CheckForNull
  public File getDeployDir() {
    return deployDir;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public String getPublicRootUrl() {
    return get(SERVER_BASE_URL, SERVER_BASE_URL_DEFAULT_VALUE);
  }

  @Override
  public boolean isDev() {
    return settings.getBoolean("sonar.web.dev");
  }

  @Override
  public boolean isSecured() {
    return get(SERVER_BASE_URL, SERVER_BASE_URL_DEFAULT_VALUE).startsWith("https://");
  }

  private static String readVersion(String filename) throws IOException {
    URL url = ServerImpl.class.getResource(filename);
    if (url != null) {
      String version = Resources.toString(url, StandardCharsets.UTF_8);
      if (!StringUtils.isBlank(version)) {
        return StringUtils.deleteWhitespace(version);
      }
    }
    throw new IllegalStateException("Unknown SonarQube version");
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

  private String get(String key, String defaultValue) {
    return Objects.firstNonNull(settings.getString(key), defaultValue);
  }

}
