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
package org.sonar.server.database;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.derby.jdbc.ClientDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.config.Settings;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

public class EmbeddedDatabaseTest {

  private final static String TEST_ROOT_DIR = "./target/";
  private final static String TEST_DB_DIR_PREFIX = "testDB";

  private EmbeddedDatabase database;
  private String driverUrl;
  private Properties defaultProps;
  private static String testPort;

  @Before
  public void setUp() throws Exception {
    windowsCleanup();
    if (testPort == null) {
      testPort = Integer.toString(findFreeServerPort());
    }
    defaultProps = EmbeddedDatabase.getDefaultProperties(new Settings());
    defaultProps.put("derby.drda.portNumber", testPort); // changing the defaut port
    driverUrl = "jdbc:derby://localhost:" + testPort + "/sonar;create=true;user=sonar;password=sonar";
  }

  private void windowsCleanup() {
    String os = System.getProperty("os.name");
    if (os.toLowerCase().contains("windows")) {
      File testRoot = new File(TEST_ROOT_DIR);
      File[] files = testRoot.listFiles();
      for (File file : files) {
        if (file.isDirectory() &&
            file.getName().startsWith(TEST_DB_DIR_PREFIX)) {
          try {
            FileUtils.deleteDirectory(file);
          } catch (IOException e) {
          }
        }
      }
    }
  }

  private int findFreeServerPort() throws IOException, InterruptedException {
    ServerSocket srv = new ServerSocket(0);
    int port = srv.getLocalPort();
    srv.close();
    Thread.sleep(1500);
    return port;
  }

  @Ignore
  @Test
  public void shouldStart() throws Exception {
    database = new EmbeddedDatabase(new File(TEST_ROOT_DIR + TEST_DB_DIR_PREFIX + "Start" + testPort), defaultProps);
    database.start();
    ClientDriver.class.newInstance();
    Connection conn;
    try {
      conn = DriverManager.getConnection(driverUrl);
      conn.close();
    } catch (Exception ex) {
      fail("Unable to connect");
    }
    try {
      conn = DriverManager.getConnection("jdbc:derby://localhost:" + testPort + "/sonar;user=foo;password=bar");
      conn.close();
      fail("Able to connect");
    } catch (Exception ex) {
    }

    File testDb = new File(database.getDataDir(), "sonar");
    assertTrue(testDb.exists());
    assertTrue(testDb.isDirectory());

    database.stop();
  }

  @Test
  public void shouldStop() throws Exception {
    database = new EmbeddedDatabase(new File(TEST_ROOT_DIR + TEST_DB_DIR_PREFIX + "Stop" + testPort), defaultProps);
    database.start();
    ClientDriver.class.newInstance();
    Connection conn;
    try {
      conn = DriverManager.getConnection(driverUrl);
      conn.close();
    } catch (Exception ex) {
      fail("Unable to connect to " + driverUrl);
    }

    database.stop();
    try {
      conn = DriverManager.getConnection(driverUrl);
      conn.close();
      fail("Able to connect");
    } catch (Exception ex) {
    }
  }

  @After
  public void tearDown() throws IOException {
    if (database.getDataDir().exists()) {
      if (!SystemUtils.IS_OS_WINDOWS) {
        // avoid an issue with file lock issue under windows..
        // thank you mr microsoft
        // solution : no really good solution found.., the db home is not deleted under windows on teardown but only during test startup
        FileUtils.deleteDirectory(database.getDataDir());
      }
    }
  }

}
