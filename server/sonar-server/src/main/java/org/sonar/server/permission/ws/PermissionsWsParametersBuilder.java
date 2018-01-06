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
package org.sonar.server.permission.ws;

import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.core.util.Uuids;

import static java.lang.String.format;
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

public class PermissionsWsParametersBuilder {

  private static final String PERMISSION_PARAM_DESCRIPTION = format("Permission" +
    "<ul>" +
    "<li>Possible values for global permissions: %s</li>" +
    "<li>Possible values for project permissions %s</li>" +
    "</ul>",
    GlobalPermissions.ALL_ON_ONE_LINE,
    ProjectPermissions.ALL_ON_ONE_LINE);
  public static final String PROJECT_PERMISSION_PARAM_DESCRIPTION = format("Permission" +
    "<ul>" +
    "<li>Possible values for project permissions %s</li>" +
    "</ul>",
    ProjectPermissions.ALL_ON_ONE_LINE);

  private PermissionsWsParametersBuilder() {
    // static methods only
  }

  public static NewParam createPermissionParameter(NewAction action) {
    return action.createParam(PARAM_PERMISSION)
      .setDescription(PERMISSION_PARAM_DESCRIPTION)
      .setRequired(true);
  }

  public static NewParam createProjectPermissionParameter(NewAction action, boolean required) {
    return action.createParam(PARAM_PERMISSION)
      .setDescription(PROJECT_PERMISSION_PARAM_DESCRIPTION)
      .setPossibleValues(ProjectPermissions.ALL)
      .setRequired(required);
  }

  public static NewParam createProjectPermissionParameter(NewAction action) {
    return createProjectPermissionParameter(action, true);
  }

  public static void createGroupNameParameter(NewAction action) {
    action.createParam(PARAM_GROUP_NAME)
      .setDescription("Group name or 'anyone' (case insensitive)")
      .setExampleValue("sonar-administrators");
  }

  public static NewParam createOrganizationParameter(NewAction action) {
    return action.createParam(PARAM_ORGANIZATION)
      .setDescription("Key of organization, used when group name is set")
      .setExampleValue("my-org")
      .setInternal(true);
  }

  public static void createGroupIdParameter(NewAction action) {
    action.createParam(PARAM_GROUP_ID)
      .setDescription("Group id")
      .setExampleValue("42");
  }

  public static void createProjectParameters(NewAction action) {
    createProjectIdParameter(action);
    createProjectKeyParameter(action);
  }

  private static void createProjectIdParameter(NewAction action) {
    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");
  }

  private static void createProjectKeyParameter(NewAction action) {
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  public static void createUserLoginParameter(NewAction action) {
    action.createParam(PARAM_USER_LOGIN)
      .setRequired(true)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }

  public static void createTemplateParameters(NewAction action) {
    createTemplateIdParameter(action);
    createOrganizationParameter(action);
    createTemplateNameParameter(action);
  }

  private static void createTemplateIdParameter(NewAction action) {
    action.createParam(PARAM_TEMPLATE_ID)
      .setDescription("Template id")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
  }

  private static void createTemplateNameParameter(NewAction action) {
    action.createParam(PARAM_TEMPLATE_NAME)
      .setDescription("Template name")
      .setExampleValue("Default Permission Template for Projects");
  }

  public static void createTemplateProjectKeyPatternParameter(NewAction action) {
    action.createParam(PARAM_PROJECT_KEY_PATTERN)
      .setDescription("Project key pattern. Must be a valid Java regular expression")
      .setExampleValue(".*\\.finance\\..*");
  }

  public static void createTemplateDescriptionParameter(NewAction action) {
    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setExampleValue("Permissions for all projects related to the financial service");
  }

  public static void createIdParameter(NewAction action) {
    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("Id")
      .setExampleValue("af8cb8cc-1e78-4c4e-8c00-ee8e814009a5");
  }
}
