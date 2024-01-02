/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.concurrent.CountDownLatch;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;

import static org.sonar.core.util.FileUtils.deleteQuietly;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;

class EmbeddedTomcat {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedTomcat.class);

  private final Props props;
  private final TomcatHttpConnectorFactory tomcatHttpConnectorFactory;
  private Tomcat tomcat = null;
  private volatile StandardContext webappContext;
  private final CountDownLatch stopLatch = new CountDownLatch(1);

  EmbeddedTomcat(Props props, TomcatHttpConnectorFactory tomcatHttpConnectorFactory) {
    this.props = props;
    this.tomcatHttpConnectorFactory = tomcatHttpConnectorFactory;
  }

  void start() {
    // '%2F' (slash /) and '%5C' (backslash \) are permitted as path delimiters in URLs
    System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

    System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
    // prevent Tomcat from shutting down our logging when stopping
    System.setProperty("logbackDisableServletContainerInitializer", "true");

    tomcat = new Tomcat();
    // Initialize directories
    String basedir = tomcatBasedir().getAbsolutePath();
    tomcat.setBaseDir(basedir);
    tomcat.getHost().setAppBase(basedir);
    tomcat.getHost().setAutoDeploy(false);
    tomcat.getHost().setCreateDirs(false);
    tomcat.getHost().setDeployOnStartup(true);
    new TomcatErrorHandling().configure(tomcat);
    new TomcatAccessLog().configure(tomcat, props);
    tomcat.getService().addConnector(tomcatHttpConnectorFactory.createConnector(props));
    webappContext = new TomcatContexts().configure(tomcat, props);
    try {
      tomcat.start();
      validateConnectorScheme();
    } catch (LifecycleException e) {
      LOGGER.error("Failed to start web server", e);
      Throwables.propagate(e);
    }
  }

  private File tomcatBasedir() {
    return new File(props.value(PATH_TEMP.getKey()), "tc");
  }

  private void validateConnectorScheme() {
    Connector[] connectors = tomcat.getService().findConnectors();
    for (Connector connector : connectors) {
      if (!connector.getScheme().equals("http")) {
        throw new IllegalArgumentException("Unsupported connector: " + connector);
      }
    }
  }

  Status getStatus() {
    if (webappContext == null) {
      return Status.DOWN;
    }
    return switch (webappContext.getState()) {
      case NEW, INITIALIZING, INITIALIZED, STARTING_PREP, STARTING -> Status.DOWN;
      case STARTED -> Status.UP;
      default ->
        // problem, stopped or failed
        Status.FAILED;
    };
  }

  public enum Status {
    DOWN, UP, FAILED
  }

  void terminate() {
    try {
      if (tomcat.getServer().getState().isAvailable()) {
        try {
          tomcat.stop();
          tomcat.destroy();
        } catch (Exception e) {
          LOGGER.warn("Failed to stop web server", e);
        }
      }
      deleteQuietly(tomcatBasedir());
    } finally {
      stopLatch.countDown();
    }
  }

  void awaitTermination() {
    try {
      // calling tomcat.getServer().await() might block forever if stop fails for whatever reason
      stopLatch.await();
    } catch (InterruptedException e) {
      // quit
    }
  }
}
