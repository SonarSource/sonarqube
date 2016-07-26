/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Throwables;
import javax.servlet.ServletContextEvent;
import org.jruby.rack.RackApplicationFactory;
import org.jruby.rack.rails.RailsServletContextListener;
import org.jruby.rack.servlet.ServletRackContext;

/**
 * Overriding {@link RailsServletContextListener} allows to disable initialization of Ruby on Rails
 * environment when Java components fail to start.
 * See https://jira.sonarsource.com/browse/SONAR-6740
 */
public class RubyRailsContextListener extends RailsServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    if (event.getServletContext().getAttribute(PlatformServletContextListener.STARTED_ATTRIBUTE) != null) {
      super.contextInitialized(event);
    }
  }

  // Always stop server when an error is raised during startup.
  // By default Rack only logs an error (see org.jruby.rack.RackServletContextListener#handleInitializationException()).
  // Rack propagates exceptions if the properties jruby.rack.exception or jruby.rack.error are set to true.
  // Unfortunately we didn't succeed in defining these properties, so the method is overridden here.
  // Initial need: SONAR-6171
  @Override
  protected void handleInitializationException(Exception e, RackApplicationFactory factory, ServletRackContext rackContext) {
    throw Throwables.propagate(e);
  }
}
