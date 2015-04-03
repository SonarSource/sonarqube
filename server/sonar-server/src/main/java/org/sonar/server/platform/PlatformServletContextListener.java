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
package org.sonar.server.platform;

import com.google.common.base.Throwables;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Enumeration;
import java.util.Properties;

public final class PlatformServletContextListener implements ServletContextListener {

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
      Platform.getInstance().init(props, servletContext);
      Platform.getInstance().doStart();
    } catch (Throwable t) {
      // Tomcat 7 "limitations":
      // - server does not stop if webapp fails at startup
      // - the second listener for jruby on rails is started even if this listener fails. It generates
      // unexpected errors
      stopQuietly();
      throw Throwables.propagate(t);
    }
  }

  private void stopQuietly() {
    try {
      Platform.getInstance().doStop();
    } catch (Exception e) {
      // ignored, but an error during startup generally prevents pico to be correctly stopped
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    Platform.getInstance().doStop();
  }

}
