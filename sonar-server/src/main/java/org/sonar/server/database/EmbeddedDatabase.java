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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.server.platform.ServerStartException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Properties;

public class EmbeddedDatabase {

  private static final String DEFAULT_USER = "sonar";
  private static final String DEFAULT_PWD = "sonar";

  private NetworkServerControl serverControl = null;

  private File dbHome;
  private Properties dbProps;
  private PrintWriter dbLog;

  public EmbeddedDatabase(Settings settings) {
    this.dbHome = getDataDirectory(settings);
    this.dbProps = getDefaultProperties(settings);
  }

  public EmbeddedDatabase(File dbHome, Properties dbProps) {
    this.dbHome = dbHome;
    this.dbProps = dbProps;
  }

  public File getDataDir() {
    return dbHome;
  }

  protected File getDataDirectory(Settings settings) {
    String dirName = settings.getString(DatabaseProperties.PROP_EMBEDDED_DATA_DIR);
    if (StringUtils.isBlank(dirName)) {
      File sonarHome = new File(settings.getString(CoreProperties.SONAR_HOME));
      if (!sonarHome.isDirectory() || !sonarHome.exists()) {
        throw new ServerStartException("Sonar home directory does not exist");
      }
      return new File(sonarHome, "data");
    }
    return new File(dirName);
  }

  public void setDbLog(PrintWriter dbLog) {
    this.dbLog = dbLog;
  }

  public void start() {
    if (dbHome.exists() && !dbHome.isDirectory()) {
      throw new SonarException("Database home " + dbHome.getPath() + " is not a directory");
    }
    if (!dbHome.exists()) {
      dbHome.mkdirs();
    }
    System.setProperty("derby.system.home", dbHome.getPath());

    saveDerbyPropertiesFile();
    startListening();
    Logs.INFO.info("Embedded database started. Data stored in: " + dbHome.getAbsolutePath());
  }

  private void startListening() {
    try {
      int port = Integer.parseInt(dbProps.getProperty("derby.drda.portNumber"));
      String host = dbProps.getProperty("derby.drda.host");
      serverControl = new NetworkServerControl(InetAddress.getByName(host), port, DEFAULT_USER, DEFAULT_PWD);
      Logs.INFO.info("Starting embedded database on port " + port);
      serverControl.start(dbLog);
      ensureServerIsUp();
    } catch (Exception e) {
      throw new SonarException(e);
    }
  }

  private void saveDerbyPropertiesFile() {
    FileOutputStream output = null;
    try {
      File derbyProps = new File(dbHome.getPath() + "/derby.properties");
      output = new FileOutputStream(derbyProps);
      dbProps.store(output, "GENERATED FILE, DO NOT EDIT ME UNLESS YOU WANT TO LOOSE YOUR TIME ;O)");

    } catch (IOException e) {
      throw new SonarException(e);

    } finally {
      IOUtils.closeQuietly(output);
    }
  }

  public void stop() {
    if (serverControl != null) {
      try {
        serverControl.shutdown();
        ensureServerIsDown();
        serverControl = null;
        Logs.INFO.info("Embedded database stopped");

      } catch (Exception e) {
        throw new SonarException(e);
      }
    }
  }

  private void ensureServerIsUp() {
    for (int retry = 0; retry < 100; retry++) {
      try {
        serverControl.ping();
        return;

      } catch (Exception ex) {
        sleep(300);
      }
    }
    throw new SonarException("Embedded database does not respond to ping requests");
  }

  private void ensureServerIsDown() {
    for (int retry = 0; retry < 100; retry++) {
      try {
        serverControl.ping();
        sleep(300);

      } catch (SonarException se) {
        throw se;

      } catch (Exception e) {
        // normal case: the database does not respond to ping
        return;
      }
    }
    throw new SonarException("Fail to stop embedded database");
  }


  private void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      throw new SonarException("Fail to ping embedded database", e);
    }
  }

  public static Properties getDefaultProperties(Settings settings) {
    Properties props = new Properties();
    props.setProperty("derby.drda.startNetworkServer", "true");
    props.setProperty("derby.drda.host", StringUtils.defaultIfBlank(settings.getString("sonar.derby.drda.host"), "localhost"));
    props.setProperty("derby.drda.portNumber", StringUtils.defaultIfBlank(settings.getString("sonar.derby.drda.portNumber"), "1527"));
    props.setProperty("derby.drda.maxThreads", StringUtils.defaultIfBlank(settings.getString("sonar.derby.drda.maxThreads"), "20"));
    props.setProperty("derby.drda.minThreads", StringUtils.defaultIfBlank(settings.getString("sonar.derby.drda.minThreads"), "2"));
    props.setProperty("derby.drda.logConnections", StringUtils.defaultIfBlank(settings.getString("sonar.derby.drda.logConnections"), "false"));
    props.setProperty("derby.stream.error.logSeverityLevel", StringUtils.defaultIfBlank(settings.getString("sonar.derby.stream.error.logSeverityLevel"), "20000"));
    props.setProperty("derby.connection.requireAuthentication", "true");
    props.setProperty("derby.user." + DEFAULT_USER, DEFAULT_PWD);
    return props;
  }

}
