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
package org.sonar.server.platform.web;

import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.platform.PlatformImpl;

public final class PlatformServletContextListener implements ServletContextListener {
  static final String STARTED_ATTRIBUTE = "sonarqube.started";
  private static final Logger LOG = LoggerFactory.getLogger(PlatformServletContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent event) {
    try {
      Properties props = new Properties();
      ServletContext servletContext = event.getServletContext();
      Enumeration<String> paramKeys = servletContext.getInitParameterNames();
      while (paramKeys.hasMoreElements()) {
        String key = paramKeys.nextElement();
        props.put(key, servletContext.getInitParameter(key));
      }
      PlatformImpl.getInstance().init(props, servletContext);
      PlatformImpl.getInstance().doStart();
      event.getServletContext().setAttribute(STARTED_ATTRIBUTE, Boolean.TRUE);
    } catch (org.sonar.api.utils.MessageException | org.sonar.process.MessageException e) {
      LOG.error("Web server startup failed: {}", e.getMessage());
      stopQuietly();
    } catch (Throwable t) {
      LOG.error("Web server startup failed", t);
      stopQuietly();
      throw new AbortTomcatStartException();
    }
  }

  private static void stopQuietly() {
    try {
      PlatformImpl.getInstance().doStop();
    } catch (Exception e) {
      // ignored, but an error during startup generally prevents pico to be correctly stopped
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    PlatformImpl.getInstance().doStop();
  }

}
