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

import com.google.common.base.Throwables;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import java.io.File;

class EmbeddedTomcat {

  private final Props props;
  private Tomcat tomcat = null;
  private volatile StandardContext webappContext;

  EmbeddedTomcat(Props props) {
    this.props = props;
  }

  void start() {
    // '%2F' (slash /) and '%5C' (backslash \) are permitted as path delimiters in URLs
    // See Ruby on Rails url_for
    System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

    System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");

    tomcat = new Tomcat();
    // Initialize directories
    String basedir = tomcatBasedir().getAbsolutePath();
    tomcat.setBaseDir(basedir);
    tomcat.getHost().setAppBase(basedir);
    tomcat.getHost().setAutoDeploy(false);
    tomcat.getHost().setCreateDirs(false);
    tomcat.getHost().setDeployOnStartup(true);
    new TomcatAccessLog().configure(tomcat, props);
    TomcatConnectors.configure(tomcat, props);
    webappContext = Webapp.configure(tomcat, props);
    try {
      tomcat.start();
      new StartupLogs(props, Loggers.get(getClass())).log(tomcat);
    } catch (LifecycleException e) {
      Throwables.propagate(e);
    }
  }

  boolean isReady() {
    if (webappContext == null) {
      return false;
    }
    switch (webappContext.getState()) {
      case NEW:
      case INITIALIZING:
      case INITIALIZED:
      case STARTING_PREP:
      case STARTING:
        return false;
      case STARTED:
        return true;
      default:
        // problem, stopped or failed
        throw new IllegalStateException("Webapp did not start");
    }
  }

  private File tomcatBasedir() {
    return new File(props.value(ProcessProperties.PATH_TEMP), "tc");
  }

  void terminate() {
    if (tomcat.getServer().getState().isAvailable()) {
      try {
        tomcat.stop();
        tomcat.destroy();
      } catch (Exception e) {
        Loggers.get(EmbeddedTomcat.class).error("Fail to stop web server", e);
      }
    }
    FileUtils.deleteQuietly(tomcatBasedir());
  }

  void awaitTermination() {
    tomcat.getServer().await();
  }
}
