/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.setting.ws;

import java.util.Optional;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.process.ProcessProperties;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.sonar.api.web.UserRole.ADMIN;

@ServerSide
public class SettingsWsSupport {
  public static final String DOT_SECURED = ".secured";

  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final UserSession userSession;

  public SettingsWsSupport(DefaultOrganizationProvider defaultOrganizationProvider, UserSession userSession) {
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.userSession = userSession;
  }

  static void validateKey(String key) {
    stream(ProcessProperties.Property.values())
      .filter(property -> property.getKey().equalsIgnoreCase(key))
      .findFirst()
      .ifPresent(property -> {
        throw new IllegalArgumentException(format("Setting '%s' can only be used in sonar.properties", key));
      });
  }

  boolean isVisible(String key, Optional<ComponentDto> component) {
    return hasPermission(OrganizationPermission.SCAN, UserRole.SCAN, component) || verifySecuredSetting(key, component);
  }

  static boolean isSecured(String key) {
    return key.endsWith(DOT_SECURED);
  }

  private boolean verifySecuredSetting(String key, Optional<ComponentDto> component) {
    return (!isSecured(key) || hasPermission(OrganizationPermission.ADMINISTER, ADMIN, component));
  }

  private boolean hasPermission(OrganizationPermission orgPermission, String projectPermission, Optional<ComponentDto> component) {
    if (userSession.isSystemAdministrator()) {
      return true;
    }
    if (userSession.hasPermission(orgPermission, defaultOrganizationProvider.get().getUuid())) {
      return true;
    }
    return component
      .map(c -> userSession.hasPermission(orgPermission, c.getOrganizationUuid()) || userSession.hasComponentPermission(projectPermission, c))
      .orElse(false);
  }

}
