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
package org.sonar.server.setting.ws;

import java.util.Optional;
import java.util.function.Predicate;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.user.UserSession;

import static org.sonar.api.web.UserRole.ADMIN;

public class SettingsPermissionPredicates {

  private static final String SECURED_SUFFIX = ".secured";
  static final String LICENSE_SUFFIX = ".license.secured";
  static final String LICENSE_HASH_SUFFIX = ".licenseHash.secured";

  private final UserSession userSession;

  public SettingsPermissionPredicates(UserSession userSession) {
    this.userSession = userSession;
  }

  Predicate<Setting> isSettingVisible(Optional<ComponentDto> component) {
    return setting -> isVisible(setting.getKey(), component);
  }

  Predicate<PropertyDefinition> isDefinitionVisible(Optional<ComponentDto> component) {
    return propertyDefinition -> isVisible(propertyDefinition.key(), component);
  }

  boolean isVisible(String settingKey, Optional<ComponentDto> component) {
    return !settingKey.endsWith(SECURED_SUFFIX)
      || hasAdminPermission(component)
      || (isLicenseRelated(settingKey) && userSession.isLoggedIn());
  }

  private boolean hasAdminPermission(Optional<ComponentDto> component) {
    return component.isPresent() ? userSession.hasComponentUuidPermission(ADMIN, component.get().uuid()) : userSession.hasPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private static boolean isLicenseRelated(String settingKey) {
    return settingKey.endsWith(LICENSE_SUFFIX) || settingKey.endsWith(LICENSE_HASH_SUFFIX);
  }
}
