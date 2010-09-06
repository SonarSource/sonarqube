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
import org.apache.commons.lang.StringUtils;
import org.jruby.rack.rails.RailsServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.configuration.ConfigurationFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public final class PlatformLifecycleListener extends RailsServletContextListener {

  private static final Logger LOG = LoggerFactory.getLogger(PlatformLifecycleListener.class);

  @Override
  public void contextInitialized(ServletContextEvent event) {
    Configuration configuration = new ConfigurationFactory().getConfiguration(event);
    Platform.getInstance().init(configuration);
    Platform.getInstance().start();

    ServletContextHandler handler = new ServletContextHandler(event.getServletContext(), configuration);
    ServletContext ctxProxy = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class[]{ServletContext.class}, handler);
    super.contextInitialized(new ServletContextEvent(ctxProxy));
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    Platform.getInstance().stop();
    super.contextDestroyed(event);
  }

  private static final class ServletContextHandler implements InvocationHandler {

    private ServletContext context;
    private String runtimeMode;

    private ServletContextHandler(ServletContext context, Configuration config) {
      this.context = context;
      runtimeMode = config.getString("sonar.runtime.mode", "production");
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
      Object value = method.invoke(context, args);
      if (StringUtils.equals("getInitParameter", method.getName())) {
        String attrName = (String) args[0];
        if (LOG.isDebugEnabled()) {
          LOG.debug("Ctx init param {}={}", attrName, value);
        }
        if (StringUtils.equals("rails.env", attrName)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Override rails.env to {}", runtimeMode);
          }
          LOG.info("Runtime mode set to {}", runtimeMode);
          return runtimeMode;
        }
      }
      return value;
    }
  }

}
