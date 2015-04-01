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
package org.sonar.server.ruby;

import org.jruby.Ruby;
import org.jruby.rack.RackApplicationFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * Implementation of {@link RackBridge} which get access to the Rack object through the {@link ServletContext}.
 */
public class PlatformRackBridge implements RackBridge {
  public static final String RACK_FACTORY_ATTR_KEY = "rack.factory";
  private final ServletContext servletContext;

  public PlatformRackBridge(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * From {@link org.jruby.rack.RackServletContextListener#contextInitialized(ServletContextEvent)} implementation, we
   * know that the {@link RackApplicationFactory} is stored in the {@link ServletContext} under the attribute key
   * {@link #RACK_FACTORY_ATTR_KEY}.
   *
   * @return a {@link Ruby} object representing the runtime environment of the RoR application of the platform.
   *
   * @throws RuntimeException if the {@link RackApplicationFactory} can not be retrieved
   */
  @Override
  public Ruby getRubyRuntime() {
    Object attribute = servletContext.getAttribute(RACK_FACTORY_ATTR_KEY);
    if (!(attribute instanceof RackApplicationFactory)) {
      throw new RuntimeException("Can not retrieve the RackApplicationFactory from ServletContext. " +
        "Ruby runtime can not be retrieved");
    }

    RackApplicationFactory rackApplicationFactory = (RackApplicationFactory) attribute;
    return rackApplicationFactory.getApplication().getRuntime();
  }
}
