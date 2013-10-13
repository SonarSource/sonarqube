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

import java.io.File;
import java.util.logging.LogManager;

class Logging {

  static final String CONF_PATH = "conf/logback-access.xml";

  static void init() {
    // Configure java.util.logging, used by Tomcat, in order to forward to slf4j
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
  }

  static void configure(Tomcat tomcat, Env env) {
    tomcat.setSilent(false);
    tomcat.getService().addLifecycleListener(new StartupLogger(LoggerFactory.getLogger(Logging.class)));

    LogbackValve valve = new LogbackValve();
    valve.setQuiet(true);
    File confFile = env.file(CONF_PATH);
    if (!confFile.exists()) {
      throw new IllegalStateException("File is missing: " + confFile.getAbsolutePath());
    }
    valve.setFilename(confFile.getAbsolutePath());
    tomcat.getHost().getPipeline().addValve(valve);
  }

  static class StartupLogger implements LifecycleListener {
    private Logger logger;

    StartupLogger(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
      if ("after_start".equals(event.getType())) {
        logger.info("Web server is up");
      }
    }
  }

}
