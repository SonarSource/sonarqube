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
package org.sonar.server.setting.ws;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.process.ProcessProperties;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_BRANCH;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;

@ServerSide
public class SettingsWsSupport {

  private static final Collector<CharSequence, ?, String> COMMA_JOINER = Collectors.joining(",");

  public static final String DOT_SECURED = ".secured";
  public static final String DOT_LICENSE = ".license";
  private static final String LICENSE_SUFFIX = DOT_LICENSE + DOT_SECURED;
  private static final String LICENSE_HASH_SUFFIX = ".licenseHash" + DOT_SECURED;

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

  Predicate<Setting> isSettingVisible(Optional<ComponentDto> component) {
    return setting -> isVisible(setting.getKey(), setting.getDefinition(), component);
  }

  Predicate<PropertyDefinition> isDefinitionVisible(Optional<ComponentDto> component) {
    return propertyDefinition -> isVisible(propertyDefinition.key(), propertyDefinition, component);
  }

  boolean isVisible(String key, @Nullable PropertyDefinition definition, Optional<ComponentDto> component) {
    return hasPermission(OrganizationPermission.SCAN, SCAN_EXECUTION, component) || (verifySecuredSetting(key, definition, component) && (verifyLicenseSetting(key, definition)));
  }

  private boolean verifySecuredSetting(String key, @Nullable PropertyDefinition definition, Optional<ComponentDto> component) {
    return isLicense(key, definition) || (!key.endsWith(DOT_SECURED) || hasPermission(OrganizationPermission.ADMINISTER, ADMIN, component));
  }

  private boolean verifyLicenseSetting(String key, @Nullable PropertyDefinition definition) {
    return !isLicense(key, definition) || userSession.isLoggedIn();
  }

  private static boolean isLicense(String key, @Nullable PropertyDefinition definition) {
    return key.endsWith(LICENSE_SUFFIX) || key.endsWith(LICENSE_HASH_SUFFIX) || (definition != null && definition.type() == LICENSE);
  }

  private boolean hasPermission(OrganizationPermission orgPermission, String projectPermission, Optional<ComponentDto> component) {
    if (userSession.hasPermission(orgPermission, defaultOrganizationProvider.get().getUuid())) {
      return true;
    }
    return component
      .map(c -> userSession.hasComponentPermission(projectPermission, c))
      .orElse(false);
  }

  WebService.NewParam addBranchParam(WebService.NewAction action) {
    return action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Only available on following settings : %s", SettingsWs.SETTING_ON_BRANCHES.stream().collect(COMMA_JOINER))
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setInternal(true)
      .setSince("6.6");
  }
}
