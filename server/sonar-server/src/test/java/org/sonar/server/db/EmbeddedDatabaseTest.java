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
package org.sonar.server.db;

import org.h2.Driver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessProperties;

import java.io.File;
import java.sql.DriverManager;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedDatabaseTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test(timeout = 10000)
  public void should_start_and_stop() {
    int port = NetworkUtils.freePort();

    EmbeddedDatabase database = new EmbeddedDatabase(testSettings(port));
    database.start();

    try {
      String driverUrl = String.format("jdbc:h2:tcp://localhost:%d/sonar;USER=login;PASSWORD=pwd", port);
      DriverManager.registerDriver(new Driver());
      DriverManager.getConnection(driverUrl).close();
    } catch (Exception ex) {
      fail("Unable to connect after start");
    }

    database.stop();
  }

  @Test
  public void should_return_embedded_data_directory() {
    Settings settings = testSettings(0);
    EmbeddedDatabase database = new EmbeddedDatabase(settings);

    File dataDirectory = database.getDataDirectory(settings);
    assertThat(dataDirectory).isNotNull();
    assertThat(dataDirectory.getPath()).endsWith("testDB");
  }

  static Settings testSettings(int port) {
    return new Settings()
      .setProperty(DatabaseProperties.PROP_USER, "login")
      .setProperty(DatabaseProperties.PROP_PASSWORD, "pwd")
      .setProperty(DatabaseProperties.PROP_EMBEDDED_PORT, "" + port)
      .setProperty(ProcessProperties.PATH_DATA, "./target/testDB");
  }
}
