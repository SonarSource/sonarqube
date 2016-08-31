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
package org.sonar.server.platform.db;

import java.io.IOException;
import java.sql.DriverManager;
import org.h2.Driver;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.process.NetworkUtils;

import static junit.framework.Assert.fail;
import static org.sonar.api.database.DatabaseProperties.PROP_EMBEDDED_PORT;
import static org.sonar.api.database.DatabaseProperties.PROP_PASSWORD;
import static org.sonar.api.database.DatabaseProperties.PROP_PASSWORD_DEFAULT_VALUE;
import static org.sonar.api.database.DatabaseProperties.PROP_URL;
import static org.sonar.api.database.DatabaseProperties.PROP_USER;
import static org.sonar.api.database.DatabaseProperties.PROP_USER_DEFAULT_VALUE;
import static org.sonar.process.ProcessProperties.PATH_DATA;

public class EmbeddedDatabaseTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public Timeout timeout = Timeout.seconds(10);

  private EmbeddedDatabase underTest;

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void start_fails_with_IAE_if_property_Data_Path_is_not_set() {
    EmbeddedDatabase underTest = new EmbeddedDatabase(new MapSettings());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property " + PATH_DATA);

    underTest.start();
  }

  @Test
  public void start_fails_with_IAE_if_property_Data_Path_is_empty() {
    EmbeddedDatabase underTest = new EmbeddedDatabase(new MapSettings()
      .setProperty(PATH_DATA, ""));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property " + PATH_DATA);

    underTest.start();
  }

  @Test
  public void start_fails_with_IAE_if_JDBC_URL_settings_is_not_set() throws IOException {
    EmbeddedDatabase underTest = new EmbeddedDatabase(new MapSettings()
      .setProperty(PATH_DATA, temporaryFolder.newFolder().getAbsolutePath()));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property " + PROP_URL);

    underTest.start();
  }

  @Test
  public void start_fails_with_IAE_if_embedded_port_settings_is_not_set() throws IOException {
    EmbeddedDatabase underTest = new EmbeddedDatabase(new MapSettings()
      .setProperty(PATH_DATA, temporaryFolder.newFolder().getAbsolutePath())
      .setProperty(PROP_URL, "jdbc url"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property " + PROP_EMBEDDED_PORT);

    underTest.start();
  }

  @Test
  public void start_ignores_URL_to_create_database_and_uses_default_username_and_password_when_then_are_not_set() throws IOException {
    int port = NetworkUtils.freePort();
    underTest = new EmbeddedDatabase(new MapSettings()
      .setProperty(PATH_DATA, temporaryFolder.newFolder().getAbsolutePath())
      .setProperty(PROP_URL, "jdbc url")
      .setProperty(PROP_EMBEDDED_PORT, "" + port));

    underTest.start();

    checkDbIsUp(port, PROP_USER_DEFAULT_VALUE, PROP_PASSWORD_DEFAULT_VALUE);
  }

  @Test
  public void start_creates_db_with_specified_user_and_password() throws IOException {
    int port = NetworkUtils.freePort();
    underTest = new EmbeddedDatabase(new MapSettings()
      .setProperty(PATH_DATA, temporaryFolder.newFolder().getAbsolutePath())
      .setProperty(PROP_URL, "jdbc url")
      .setProperty(PROP_EMBEDDED_PORT, "" + port)
      .setProperty(PROP_USER, "foo")
      .setProperty(PROP_PASSWORD, "bar"));

    underTest.start();

    checkDbIsUp(port, "foo", "bar");
  }

  @Test
  public void start_supports_in_memory_H2_JDBC_URL() throws IOException {
    int port = NetworkUtils.freePort();
    underTest = new EmbeddedDatabase(new MapSettings()
      .setProperty(PATH_DATA, temporaryFolder.newFolder().getAbsolutePath())
      .setProperty(PROP_URL, "jdbc:h2:mem:sonar")
      .setProperty(PROP_EMBEDDED_PORT, "" + port)
      .setProperty(PROP_USER, "foo")
      .setProperty(PROP_PASSWORD, "bar"));

    underTest.start();

    checkDbIsUp(port, "foo", "bar");
  }

  private void checkDbIsUp(int port, String user, String password) {
    try {
      String driverUrl = String.format("jdbc:h2:tcp://localhost:%d/sonar;USER=%s;PASSWORD=%s", port, user, password);
      DriverManager.registerDriver(new Driver());
      DriverManager.getConnection(driverUrl).close();
    } catch (Exception ex) {
      fail("Unable to connect after start");
    }
  }

}
