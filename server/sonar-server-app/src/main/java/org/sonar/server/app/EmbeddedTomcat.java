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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;

import java.io.File;

class EmbeddedTomcat {

  public static final String TEMP_RELATIVE_PATH = "temp/tomcat";

  private final Env env;
  private Tomcat tomcat = null;
  private Thread hook = null;
  private boolean stopping = false, ready = false;

  EmbeddedTomcat(Env env) {
    this.env = env;
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
      String basedir = env.freshDir(TEMP_RELATIVE_PATH).getCanonicalPath();
      tomcat.setBaseDir(basedir);
      tomcat.getHost().setAppBase(basedir);
      tomcat.getHost().setAutoDeploy(false);
      tomcat.getHost().setCreateDirs(false);
      tomcat.getHost().setDeployOnStartup(true);

      Logging.configure(tomcat, env, env.props());
      Connectors.configure(tomcat, env.props());
      Webapp.configure(tomcat, env, env.props());
      tomcat.start();
      addShutdownHook();
      ready = true;
      tomcat.getServer().await();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to start web server", e);
    }
    // Shutdown command received
    stop();
  }

  private void addShutdownHook() {
    hook = new Thread() {
      @Override
      public void run() {
        EmbeddedTomcat.this.doStop();
      }
    };
    Runtime.getRuntime().addShutdownHook(hook);
  }


  void stop() {
    removeShutdownHook();
    doStop();
  }

  private synchronized void doStop() {
    try {
      if (tomcat != null && !stopping) {
        stopping = true;
        tomcat.stop();
        tomcat.destroy();
      }
      tomcat = null;
      stopping = false;
      ready = false;
      File tempDir = env.file(TEMP_RELATIVE_PATH);
      FileUtils.deleteQuietly(tempDir);

    } catch (LifecycleException e) {
      throw new IllegalStateException("Fail to stop web server", e);
    }
  }

  private void removeShutdownHook() {
    if (hook != null && !hook.isAlive()) {
      Runtime.getRuntime().removeShutdownHook(hook);
      hook = null;
    }
  }

  boolean isReady( ){
    return ready;
  }

  int port() {
    Connector[] connectors = tomcat.getService().findConnectors();
    if (connectors.length > 0) {
      return connectors[0].getLocalPort();
    }
    return -1;
  }
}
