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
package org.sonar.server.platform;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.configuration.ConfigurationFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public final class PlatformLifecycleListener implements ServletContextListener {

  private static final Logger LOG = LoggerFactory.getLogger(PlatformLifecycleListener.class);

  public void contextInitialized(ServletContextEvent event) {
    Configuration configuration = new ConfigurationFactory().getConfiguration(event);
    Platform.getInstance().init(configuration);
    Platform.getInstance().start();
  }

  public void contextDestroyed(ServletContextEvent event) {
    Platform.getInstance().stop();
  }
}
