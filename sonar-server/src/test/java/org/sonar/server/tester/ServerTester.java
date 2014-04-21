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
package org.sonar.server.tester;

import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.server.platform.Platform;

import java.io.File;
import java.util.Properties;

/**
 * Entry point to implement medium tests of server components
 *
 * @since 4.3
 */
public class ServerTester extends ExternalResource {

  private File tempDir;
  private Platform platform;

  @Override
  protected void before() {
    tempDir = createTempDir();

    Properties properties = new Properties();
    properties.setProperty(CoreProperties.SONAR_HOME, tempDir.getAbsolutePath());
    properties.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:" + tempDir.getAbsolutePath() + "/h2");
    platform = new Platform();
    platform.init(properties);

    platform.doStart();
  }

  private File createTempDir() {
    try {
      // Technique to create a temp directory from a temp file
      File f = File.createTempFile("SonarQube", "");
      f.delete();
      f.mkdir();
      return f;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create temp dir", e);
    }
  }

  @Override
  protected void after() {
    platform.doStop();
    platform = null;
    FileUtils.deleteQuietly(tempDir);
  }

  public <C> C get(Class<C> component) {
    if (platform == null) {
      throw new IllegalStateException("Not started");
    }
    return platform.getContainer().getComponentByType(component);
  }
}
