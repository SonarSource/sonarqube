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
package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.sonar.process.ProcessUtils;
import org.sonar.process.Props;
import org.sonar.process.Terminable;

import java.io.File;

class EmbeddedTomcat implements Terminable {

  private final Props props;
  private Tomcat tomcat = null;
  private Thread hook = null;
  private boolean stopping = false, ready = false;

  EmbeddedTomcat(Props props) {
    this.props = props;
  }

  void start() {
    if (tomcat != null || hook != null) {
      throw new IllegalStateException("Server is already started");
    }

    try {
      // '%2F' (slash /) and '%5C' (backslash \) are permitted as path delimiters in URLs
      // See Ruby on Rails url_for
      System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

      System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");

      tomcat = new Tomcat();

      // Initialize directories
      File tomcatDir = tomcatBasedir();
      String basedir = tomcatDir.getAbsolutePath();
      tomcat.setBaseDir(basedir);
      tomcat.getHost().setAppBase(basedir);
      tomcat.getHost().setAutoDeploy(false);
      tomcat.getHost().setCreateDirs(false);
      tomcat.getHost().setDeployOnStartup(true);

      Logging.configure(tomcat, props);
      Connectors.configure(tomcat, props);
      Webapp.configure(tomcat, props);
      ProcessUtils.addSelfShutdownHook(this);
      tomcat.start();
      ready = true;
      tomcat.getServer().await();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start web server", e);
    } finally {
      // Failed to start or received a shutdown command (should never occur as shutdown port is disabled)
      terminate();
    }
  }

  private File tomcatBasedir() {
    return new File(props.of("sonar.path.temp"), "tomcat");
  }

  boolean isReady() {
    return ready && tomcat != null;
  }

  int port() {
    Connector[] connectors = tomcat.getService().findConnectors();
    if (connectors.length > 0) {
      return connectors[0].getLocalPort();
    }
    return -1;
  }

  @Override
  public void terminate() {
    if (tomcat != null && !stopping) {
      try {
        stopping = true;
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        LoggerFactory.getLogger(EmbeddedTomcat.class).error("Fail to stop web service", e);
      }
    }
    tomcat = null;
    stopping = false;
    ready = false;
    FileUtils.deleteQuietly(tomcatBasedir());
  }
}
