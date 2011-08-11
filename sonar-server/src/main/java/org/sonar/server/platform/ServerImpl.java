/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.platform;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.Server;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public final class ServerImpl extends Server {

  private String id;
  private String version;
  private final Date startedAt;
  private String key;
  private Configuration conf;

  public ServerImpl(Configuration conf) {
    this(conf, new Date());
  }

  ServerImpl(Configuration conf, Date startedAt) {
    this.conf = conf;
    this.startedAt = startedAt;
  }

  public void start() {
    try {
      id = new SimpleDateFormat("yyyyMMddHHmmss").format(startedAt);
      key = initKey(conf);
      version = loadVersionFromManifest("/META-INF/maven/org.codehaus.sonar/sonar-plugin-api/pom.properties");
      if (StringUtils.isBlank(version)) {
        throw new ServerStartException("Unknown Sonar version");
      }

    } catch (IOException e) {
      throw new ServerStartException("Can not load metadata", e);
    }
  }

  private String initKey(Configuration conf) {
    String organization = conf.getString(CoreProperties.ORGANIZATION);
    String baseUrl = conf.getString(CoreProperties.SERVER_BASE_URL, CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
    String previousKey = conf.getString(CoreProperties.SERVER_KEY);
    return new ServerKeyGenerator().generate(organization, baseUrl, previousKey);
  }

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  String loadVersionFromManifest(String pomFilename) throws IOException {
    InputStream pomFileStream = getClass().getResourceAsStream(pomFilename);
    try {
      return readVersion(pomFileStream);

    } finally {
      IOUtils.closeQuietly(pomFileStream);
    }
  }

  protected static String readVersion(InputStream pomFileStream) throws IOException {
    String result = null;
    if (pomFileStream != null) {
      Properties pomProp = new Properties();
      pomProp.load(pomFileStream);
      result = pomProp.getProperty("version");
    }
    return StringUtils.defaultIfEmpty(result, "");
  }

  public String getURL() {
    return null;
  }

  public String getKey() {
    return key;
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
