/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.web;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * This extension point must be implemented to define a new dashboard.
 *
 * @since 2.13
 * @deprecated since 6.2, this extension is ignored as dashboards have been removed
 */
@Deprecated
@ServerSide
@ExtensionPoint
public abstract class DashboardTemplate {

  /**
   * Returns the {@link Dashboard} object that represents the dashboard to use.
   *
   * @return the dashboard
   */
  public abstract Dashboard createDashboard();

  /**
   * Dashboard name
   */
  public abstract String getName();
}
