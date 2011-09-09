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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.platform.Server;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public final class ServerImpl extends Server {

  private String id;
  private String version;
  private final Date startedAt;

  /**
   * This component can't use Configuration because of startup sequence. It must be started before plugins.
   */
  private DatabaseSessionFactory dbSessionFactory;

  public ServerImpl(DatabaseSessionFactory dbSessionFactory) {
    this(dbSessionFactory, new Date());
  }

  ServerImpl(DatabaseSessionFactory dbSessionFactory, Date startedAt) {
    this.dbSessionFactory = dbSessionFactory;
    this.startedAt = startedAt;
  }

  public void start() {
    try {
      id = new SimpleDateFormat("yyyyMMddHHmmss").format(startedAt);
      version = loadVersionFromManifest("/META-INF/maven/org.codehaus.sonar/sonar-plugin-api/pom.properties");
      if (StringUtils.isBlank(version)) {
        throw new ServerStartException("Unknown Sonar version");
      }

    } catch (IOException e) {
      throw new ServerStartException("Can not load metadata", e);
    }
  }

  public String getKey() {
    DatabaseSession session = dbSessionFactory.getSession();
    Property serverKey = session.getSingleResult(Property.class, "key", CoreProperties.SERVER_KEY);
    return (serverKey!= null ? serverKey.getValue() : null);
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
