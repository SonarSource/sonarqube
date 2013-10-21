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
package org.sonar.application;

import ch.qos.logback.access.tomcat.LogbackValve;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

class Logging {

  static final String ACCESS_RELATIVE_PATH = "web/WEB-INF/config/logback-access.xml";
  static final String PROPERTY_ENABLE_ACCESS_LOGS = "sonar.web.accessLogs.enable";

  static void init() {
    // Configure java.util.logging, used by Tomcat, in order to forward to slf4j
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
  }

  static void configure(Tomcat tomcat, Env env, Props props) {
    tomcat.setSilent(false);
    tomcat.getService().addLifecycleListener(new LifecycleLogger(console()));
    configureLogbackAccess(tomcat, env, props);
  }

  static Logger console() {
    return LoggerFactory.getLogger("console");
  }

  private static void configureLogbackAccess(Tomcat tomcat, Env env, Props props) {
    if (props.booleanOf(PROPERTY_ENABLE_ACCESS_LOGS, true)) {
      LogbackValve valve = new LogbackValve();
      valve.setQuiet(true);
      valve.setFilename(env.file(ACCESS_RELATIVE_PATH).getAbsolutePath());
      tomcat.getHost().getPipeline().addValve(valve);
    }
  }

  static class LifecycleLogger implements LifecycleListener {
    private Logger logger;

    LifecycleLogger(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
      if ("after_start".equals(event.getType())) {
        logger.info("Web server is started");

      } else if ("after_destroy".equals(event.getType())) {
        logger.info("Web server is stopped");
      }
    }
  }

}
