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
package org.sonar.server.db;

import org.h2.Driver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.DriverManager;

import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;

public class EmbeddedDatabaseTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test(timeout = 10000)
  public void should_start_and_stop() throws IOException {
    int port = freeServerPort();

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

  @Test(timeout = 10000)
  public void should_support_memory_database() throws IOException {
    int port = freeServerPort();

    EmbeddedDatabase database = new EmbeddedDatabase(testSettings(port)
        .setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:tcp://localhost:" + port + "/mem:sonarIT;USER=sonar;PASSWORD=sonar"));
    database.start();

    try {
      String driverUrl = String.format("jdbc:h2:tcp://localhost:%d/mem:sonarIT;USER=sonar;PASSWORD=sonar", port);
      DriverManager.registerDriver(new Driver());
      DriverManager.getConnection(driverUrl).close();
    } catch (Exception ex) {
      fail("Unable to connect after start");
    }

    database.stop();
  }

  @Test
  public void should_return_sonar_home_directory() throws Exception {
    Settings settings = testSettings(0);
    settings.setProperty(CoreProperties.SONAR_HOME, ".");
    settings.setProperty(DatabaseProperties.PROP_EMBEDDED_DATA_DIR, "");

    EmbeddedDatabase database = new EmbeddedDatabase(settings);

    File dataDirectory = database.getDataDirectory(settings);
    assertThat(dataDirectory).isNotNull();
    assertThat(dataDirectory.getPath()).endsWith("data");
  }

  @Test
  public void should_fail_on_invalid_sonar_home_directory() throws Exception {
    throwable.expect(IllegalStateException.class);

    String testPath = getClass().getResource(".").getPath();

    Settings settings = testSettings(0);
    settings.setProperty(CoreProperties.SONAR_HOME, testPath + "/unmatched_directory");
    settings.setProperty(DatabaseProperties.PROP_EMBEDDED_DATA_DIR, "");

    EmbeddedDatabase database = new EmbeddedDatabase(settings);
    database.getDataDirectory(settings);
  }

  @Test
  public void should_return_embedded_data_directory() throws Exception {

    Settings settings = testSettings(0);
    EmbeddedDatabase database = new EmbeddedDatabase(settings);

    File dataDirectory = database.getDataDirectory(settings);
    assertThat(dataDirectory).isNotNull();
    assertThat(dataDirectory.getPath()).endsWith("testDB");
  }

  @Test
  public void should_fail_on_invalid_data_directory() throws Exception {
    throwable.expect(SonarException.class);

    String testPath = getClass().getResource(".").getPath();

    Settings settings = testSettings(0);
    settings.setProperty(DatabaseProperties.PROP_EMBEDDED_DATA_DIR, testPath + "/invalid_db_data_file");

    EmbeddedDatabase database = new EmbeddedDatabase(settings);
    database.start();
  }

  static Settings testSettings(int port) {
    return new Settings()
        .setProperty(DatabaseProperties.PROP_USER, "login")
        .setProperty(DatabaseProperties.PROP_PASSWORD, "pwd")
        .setProperty(DatabaseProperties.PROP_EMBEDDED_PORT, "" + port)
        .setProperty(DatabaseProperties.PROP_EMBEDDED_DATA_DIR, "./target/testDB");
  }

  static int freeServerPort() throws IOException {
    ServerSocket srv = new ServerSocket(0);
    srv.close();
    return srv.getLocalPort();
  }
}
