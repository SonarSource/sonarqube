/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;

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

  public EmbeddedDatabase(Configuration configuration) {
    this.dbHome = autodetectDataDirectory(configuration);
    this.dbProps = getDefaultProperties(configuration);
  }

  public EmbeddedDatabase(File dbHome, Properties dbProps) {
    this.dbHome = dbHome;
    this.dbProps = dbProps;
  }

  public File getDataDir() {
    return dbHome;
  }

  protected File autodetectDataDirectory(Configuration configuration) {
    String dirName = configuration.getString(DatabaseProperties.PROP_EMBEDDED_DATA_DIR);
    if (dirName == null) {
      String sonarHome = configuration.getString(CoreProperties.SONAR_HOME);
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
      Logs.INFO.info("Embedded database: " + serverControl);
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
      } catch (Exception e) {
        throw new SonarException(e);
      }
      ensureServerIsDown();
      serverControl = null;
      Logs.INFO.info("Embedded database stopped.");
    }
  }

  private void ensureServerIsUp() {
    for (int retry = 0; retry < 16; retry++) {
      try {
        serverControl.ping();
        return;
      } catch (Exception ex) {
        sleep(250);
      }
    }
    throw new SonarException("Embedded database does not respond to ping requests");
  }

  private void sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
    }
  }

  private void ensureServerIsDown() {
    for (int retry = 0; retry < 16; retry++) {
      try {
        serverControl.ping();
        sleep(250);
      } catch (Exception ex) {
        return;
      }
    }
    throw new SonarException("Embedded database is not stopped");
  }

  public static Properties getDefaultProperties(Configuration configuration) {
    Properties props = new Properties();
    props.setProperty("derby.drda.startNetworkServer", "true");
    props.setProperty("derby.drda.host", configuration.getString("sonar.derby.drda.host", "localhost"));
    props.setProperty("derby.drda.portNumber", configuration.getString("sonar.derby.drda.portNumber", "1527"));
    props.setProperty("derby.drda.maxThreads", configuration.getString("sonar.derby.drda.maxThreads", "20"));
    props.setProperty("derby.drda.minThreads", configuration.getString("sonar.derby.drda.minThreads", "2"));
    props.setProperty("derby.drda.logConnections", configuration.getString("sonar.derby.drda.logConnections", "false"));
    props.setProperty("derby.stream.error.logSeverityLevel", configuration.getString("sonar.derby.stream.error.logSeverityLevel", "20000"));
    props.setProperty("derby.connection.requireAuthentication", "true");
    props.setProperty("derby.user." + DEFAULT_USER, DEFAULT_PWD);
    return props;
  }

}
