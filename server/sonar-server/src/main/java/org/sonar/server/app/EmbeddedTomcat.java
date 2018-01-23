/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.app;

import com.google.common.base.Throwables;
import java.io.File;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.Props;

import static org.sonar.core.util.FileUtils.deleteQuietly;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

class EmbeddedTomcat {

  private final Props props;
  private Tomcat tomcat = null;
  private volatile StandardContext webappContext;

  EmbeddedTomcat(Props props) {
    this.props = props;
  }

  void start() {
    // '%2F' (slash /) and '%5C' (backslash \) are permitted as path delimiters in URLs
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
    webappContext = new TomcatContexts().configure(tomcat, props);
    try {
      tomcat.start();
      new TomcatStartupLogs(Loggers.get(getClass())).log(tomcat);
    } catch (LifecycleException e) {
      Throwables.propagate(e);
    }
  }

  Status getStatus() {
    if (webappContext == null) {
      return Status.DOWN;
    }
    switch (webappContext.getState()) {
      case NEW:
      case INITIALIZING:
      case INITIALIZED:
      case STARTING_PREP:
      case STARTING:
        return Status.DOWN;
      case STARTED:
        return Status.UP;
      default:
        // problem, stopped or failed
        return Status.FAILED;
    }
  }

  public enum Status {
    DOWN, UP, FAILED
  }

  private File tomcatBasedir() {
    return new File(props.value(PATH_TEMP.getKey()), "tc");
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
    deleteQuietly(tomcatBasedir());
  }

  void awaitTermination() {
    tomcat.getServer().await();
  }
}
