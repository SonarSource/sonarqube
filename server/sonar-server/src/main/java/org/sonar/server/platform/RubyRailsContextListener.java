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
package org.sonar.server.platform;

import javax.servlet.ServletContextEvent;
import org.jruby.rack.rails.RailsServletContextListener;

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
}
