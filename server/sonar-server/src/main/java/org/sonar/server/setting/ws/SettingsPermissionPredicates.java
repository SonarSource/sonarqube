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
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.user.UserSession;

import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;

public class SettingsPermissionPredicates {

  public static final String DOT_SECURED = ".secured";
  public static final String DOT_LICENSE = ".license";
  private static final String LICENSE_SUFFIX = DOT_LICENSE + DOT_SECURED;
  static final String LICENSE_HASH_SUFFIX = ".licenseHash" + DOT_SECURED;

  private final UserSession userSession;

  public SettingsPermissionPredicates(UserSession userSession) {
    this.userSession = userSession;
  }

  Predicate<Setting> isSettingVisible(Optional<ComponentDto> component) {
    return setting -> isVisible(setting.getKey(), setting.getDefinition(), component);
  }

  Predicate<PropertyDefinition> isDefinitionVisible(Optional<ComponentDto> component) {
    return propertyDefinition -> isVisible(propertyDefinition.key(), propertyDefinition, component);
  }

  boolean isVisible(String key, @Nullable PropertyDefinition definition, Optional<ComponentDto> component) {
    return hasPermission(SCAN_EXECUTION, component) || (verifySecuredSetting(key, definition, component) && (verifyLicenseSetting(key, definition)));
  }

  private boolean verifySecuredSetting(String key, @Nullable PropertyDefinition definition, Optional<ComponentDto> component) {
    return isLicense(key, definition) || (!key.endsWith(DOT_SECURED) || hasPermission(ADMIN, component));
  }

  private boolean verifyLicenseSetting(String key, @Nullable PropertyDefinition definition) {
    return !isLicense(key, definition) || userSession.isLoggedIn();
  }

  private static boolean isLicense(String key, @Nullable PropertyDefinition definition) {
    return key.endsWith(LICENSE_SUFFIX) || key.endsWith(LICENSE_HASH_SUFFIX) || (definition != null && definition.type() == LICENSE);
  }

  private boolean hasPermission(String permission, Optional<ComponentDto> component) {
    return userSession.hasPermission(permission) || (component.isPresent() && userSession.hasComponentPermission(permission, component.get()));
  }
}
