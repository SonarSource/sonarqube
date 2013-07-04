/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.DriverManager;

import static junit.framework.Assert.fail;

public class EmbeddedDatabaseTest {
  @Test(timeout = 5000)
  public void should_start_and_stop() throws IOException {
    int port = freeServerPort();

    EmbeddedDatabase database = new EmbeddedDatabase(settings(port));
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

  @Test(timeout = 5000)
  public void should_support_memory_database() throws IOException {
    int port = freeServerPort();

    EmbeddedDatabase database = new EmbeddedDatabase(settings(port)
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

  static Settings settings(int port) {
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
