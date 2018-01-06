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

/**
 * @since 1.10
 * @deprecated override org.sonar.api.web.AbstractRubyTemplate and implement org.sonar.api.web.RubyRailsWidget
 */
@Deprecated
public abstract class AbstractDashboardWidget extends AbstractRubyTemplate implements RubyRailsWidget {

  @Override
  public String getId() {
    return getClass().getSimpleName();
  }

  @Override
  public String getTitle() {
    return getClass().getSimpleName();
  }
}
