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
package org.sonar.server.permission.ws;

import com.google.common.base.Joiner;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.server.permission.PermissionService;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class WsParameters {
  private final String permissionParamDescription;
  private final String projectPermissionParamDescription;

  private final PermissionService permissionService;

  public WsParameters(PermissionService permissionService) {
    this.permissionService = permissionService;
    String allProjectsPermissionsOnOneLine = Joiner.on(", ").join(permissionService.getAllProjectPermissions());
    permissionParamDescription = String.format("Permission" +
        "<ul>" +
        "<li>Possible values for global permissions: %s</li>" +
        "<li>Possible values for project permissions %s</li>" +
        "</ul>",
      GlobalPermissions.ALL_ON_ONE_LINE,
      allProjectsPermissionsOnOneLine);
    projectPermissionParamDescription = String.format("Permission" +
        "<ul>" +
        "<li>Possible values for project permissions %s</li>" +
        "</ul>",
      allProjectsPermissionsOnOneLine);
  }

  public WebService.NewParam createPermissionParameter(WebService.NewAction action) {
    return action.createParam(PARAM_PERMISSION)
      .setDescription(permissionParamDescription)
      .setRequired(true);
  }

  public WebService.NewParam createProjectPermissionParameter(WebService.NewAction action, boolean required) {
    return action.createParam(PARAM_PERMISSION)
      .setDescription(projectPermissionParamDescription)
      .setPossibleValues(permissionService.getAllProjectPermissions())
      .setRequired(required);
  }

  public WebService.NewParam createProjectPermissionParameter(WebService.NewAction action) {
    return createProjectPermissionParameter(action, true);
  }

  public static void createGroupNameParameter(WebService.NewAction action) {
    action.createParam(PARAM_GROUP_NAME)
      .setDescription("Group name or 'anyone' (case insensitive)")
      .setExampleValue("sonar-administrators");
  }

  public static WebService.NewParam createOrganizationParameter(WebService.NewAction action) {
    return action.createParam(PARAM_ORGANIZATION)
      .setDescription("Key of organization, used when group name is set")
      .setExampleValue("my-org")
      .setInternal(true);
  }

  public static void createGroupIdParameter(WebService.NewAction action) {
    action.createParam(PARAM_GROUP_ID)
      .setDescription("Group id")
      .setExampleValue("42");
  }

  public static void createProjectParameters(WebService.NewAction action) {
    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");
    createProjectKeyParameter(action);
  }

  private static void createProjectKeyParameter(WebService.NewAction action) {
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  public static void createUserLoginParameter(WebService.NewAction action) {
    action.createParam(PARAM_USER_LOGIN)
      .setRequired(true)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }

  public static void createTemplateParameters(WebService.NewAction action) {
    action.createParam(PARAM_TEMPLATE_ID)
      .setDescription("Template id")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    createOrganizationParameter(action);
    action.createParam(PARAM_TEMPLATE_NAME)
      .setDescription("Template name")
      .setExampleValue("Default Permission Template for Projects");
  }

  public static void createTemplateProjectKeyPatternParameter(WebService.NewAction action) {
    action.createParam(PARAM_PROJECT_KEY_PATTERN)
      .setDescription("Project key pattern. Must be a valid Java regular expression")
      .setExampleValue(".*\\.finance\\..*");
  }

  public static void createTemplateDescriptionParameter(WebService.NewAction action) {
    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setExampleValue("Permissions for all projects related to the financial service");
  }

  public static void createIdParameter(WebService.NewAction action) {
    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("Id")
      .setExampleValue("af8cb8cc-1e78-4c4e-8c00-ee8e814009a5");
  }
}
