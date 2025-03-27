/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.permission;

import com.google.common.base.Joiner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.component.ComponentType;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.exceptions.BadRequestException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;

public class RequestValidator {
  public static final String MSG_TEMPLATE_WITH_SAME_NAME = "A template with the name '%s' already exists (case insensitive).";
  private final String allProjectsPermissionsOnOneLine;

  public RequestValidator(PermissionService permissionService) {
    allProjectsPermissionsOnOneLine = Joiner.on(", ").join(permissionService.getAllProjectPermissions());
  }

  public String validateProjectPermission(String permission) {
    BadRequestException.checkRequest(ProjectPermission.contains(permission),
      String.format("The '%s' parameter for project permissions must be one of %s. '%s' was passed.", PARAM_PERMISSION,
        allProjectsPermissionsOnOneLine, permission));
    return permission;
  }

  public static void validateGlobalPermission(String permission) {
    checkRequest(GlobalPermission.contains(permission),
      format("The '%s' parameter for global permissions must be one of %s. '%s' was passed.", PARAM_PERMISSION, GlobalPermission.ALL_ON_ONE_LINE, permission));
  }

  public static void validateQualifier(@Nullable String qualifier, ComponentTypes componentTypes) {
    if (qualifier == null) {
      return;
    }
    Set<String> rootQualifiers = componentTypes.getRoots().stream()
      .map(ComponentType::getQualifier)
      .collect(Collectors.toSet());
    checkRequest(rootQualifiers.contains(qualifier),
      format("The '%s' parameter must be one of %s. '%s' was passed.", PARAM_QUALIFIER, rootQualifiers, qualifier));
  }

  public static void validateProjectPattern(@Nullable String projectPattern) {
    if (isNullOrEmpty(projectPattern)) {
      return;
    }

    try {
      Pattern.compile(projectPattern);
    } catch (PatternSyntaxException e) {
      throw BadRequestException.create(format("The '%s' parameter must be a valid Java regular expression. '%s' was passed", PARAM_PROJECT_KEY_PATTERN, projectPattern));
    }
  }
}
