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

import ch.qos.logback.access.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;

class TomcatAccessLog {

  public static final String PROPERTY_ENABLE = "sonar.web.accessLogs.enable";
  public static final String PROPERTY_PATTERN = "sonar.web.accessLogs.pattern";

  void configure(Tomcat tomcat, Props props) {
    tomcat.setSilent(true);
    tomcat.getService().addLifecycleListener(new LifecycleLogger(Loggers.get(TomcatAccessLog.class)));
    configureLogbackAccess(tomcat, props);
  }

  private void configureLogbackAccess(Tomcat tomcat, Props props) {
    if (props.valueAsBoolean(PROPERTY_ENABLE, true)) {
      ProgrammaticLogbackValve valve = new ProgrammaticLogbackValve();
      LogbackHelper helper = new LogbackHelper();
      LogbackHelper.RollingPolicy policy = helper.createRollingPolicy(valve, props, "access");
      FileAppender appender = policy.createAppender("ACCESS_LOG");
      PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
      fileEncoder.setContext(valve);
      fileEncoder.setPattern(props.value(PROPERTY_PATTERN, "combined"));
      fileEncoder.start();
      appender.setEncoder(fileEncoder);
      appender.start();
      valve.addAppender(appender);
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
