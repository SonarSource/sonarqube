/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import ch.qos.logback.access.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.process.Props;

class TomcatAccessLog {

  private static final String PROPERTY_ENABLE = "sonar.web.accessLogs.enable";
  private static final String PROPERTY_PATTERN = "sonar.web.accessLogs.pattern";
  private static final String DEFAULT_SQ_ACCESS_LOG_PATTERN = "%h %l %u [%t] \"%r\" %s %b \"%i{Referer}\" \"%i{User-Agent}\" \"%reqAttribute{ID}\"";

  void configure(Tomcat tomcat, Props props) {
    tomcat.setSilent(true);
    tomcat.getService().addLifecycleListener(new LifecycleLogger(Loggers.get(TomcatAccessLog.class)));
    configureLogbackAccess(tomcat, props);
  }

  private static void configureLogbackAccess(Tomcat tomcat, Props props) {
    if (props.valueAsBoolean(PROPERTY_ENABLE, true)) {
      ProgrammaticLogbackValve valve = new ProgrammaticLogbackValve();
      LogbackHelper helper = new LogbackHelper();
      LogbackHelper.RollingPolicy policy = helper.createRollingPolicy(valve, props, "access");
      FileAppender appender = policy.createAppender("ACCESS_LOG");
      PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
      fileEncoder.setContext(valve);
      fileEncoder.setPattern(props.value(PROPERTY_PATTERN, DEFAULT_SQ_ACCESS_LOG_PATTERN));
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
        logger.debug("Tomcat is started");

      } else if ("after_destroy".equals(event.getType())) {
        logger.debug("Tomcat is stopped");
      }
    }
  }

}
