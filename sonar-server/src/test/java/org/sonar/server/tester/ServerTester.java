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
import org.sonar.api.CoreProperties;
import org.sonar.server.db.migrations.DatabaseMigrator;
import org.sonar.server.platform.Platform;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

public class ServerTester {

  private File temp;

  private Platform platform;

  public ServerTester() {
    platform = new Platform();
  }

  public void start() {
    temp = new File("target/" + UUID.randomUUID().toString());
    temp.mkdirs();

    Properties properties = new Properties();
    properties.setProperty(CoreProperties.SONAR_HOME, temp.getAbsolutePath());
    properties.setProperty("sonar.jdbc.dialect", "h2");
    properties.setProperty("sonar.jdbc.url", "jdbc:h2:" + temp.getAbsolutePath() + "/h2");

    platform.init(properties);
    ((DatabaseMigrator) platform.getComponent(DatabaseMigrator.class)).createDatabase();
    platform.doStart();
  }

  public void stop() {
    platform.doStop();
    FileUtils.deleteQuietly(temp);
  }

  public <E> E get(Class<E> component) {
    return platform.getContainer().getComponentByType(component);
  }
}
