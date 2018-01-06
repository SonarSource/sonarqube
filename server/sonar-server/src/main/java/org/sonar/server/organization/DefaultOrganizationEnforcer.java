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
package org.sonar.server.organization;

import org.picocontainer.Startable;

public class DefaultOrganizationEnforcer implements Startable {
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DefaultOrganizationEnforcer(DefaultOrganizationProvider defaultOrganizationProvider) {
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void start() {
    // get will fail with IllegalStateException if no default organization can be found
    defaultOrganizationProvider.get();
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
