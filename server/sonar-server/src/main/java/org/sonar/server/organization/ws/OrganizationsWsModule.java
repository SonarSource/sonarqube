/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.organization.ws;

import org.sonar.api.config.Configuration;
import org.sonar.core.platform.Module;

import static org.sonar.core.config.WebConstants.SONARCLOUD_ENABLED;

public class OrganizationsWsModule extends Module {

  private final Configuration config;

  public OrganizationsWsModule(Configuration config) {
    this.config = config;
  }

  @Override
  protected void configureModule() {
    add(
      OrganizationsWs.class,
      OrganizationsWsSupport.class,
      // actions
      SearchAction.class,
      SearchMembersAction.class,
      SearchMyOrganizationsAction.class);
    if (config.getBoolean(SONARCLOUD_ENABLED).orElse(false)) {
      add(
        EnableSupportAction.class,
        AddMemberAction.class,
        CreateAction.class,
        DeleteAction.class,
        RemoveMemberAction.class,
        UpdateAction.class,
        UpdateProjectVisibilityAction.class);
    }
  }

}
