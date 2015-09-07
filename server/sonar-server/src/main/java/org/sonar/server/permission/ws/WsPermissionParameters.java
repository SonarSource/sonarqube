/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.core.util.Uuids;

import static java.lang.String.format;

class WsPermissionParameters {

  static final String PARAM_PERMISSION = "permission";
  static final String PARAM_GROUP_NAME = "groupName";
  static final String PARAM_GROUP_ID = "groupId";
  static final String PARAM_PROJECT_ID = "projectId";
  static final String PARAM_PROJECT_KEY = "projectKey";
  static final String PARAM_USER_LOGIN = "login";
  static final String PARAM_TEMPLATE_ID = "templateId";
  static final String PARAM_TEMPLATE_NAME = "templateName";

  static final String PARAM_ID = "id";
  static final String PARAM_NAME = "name";
  static final String PARAM_DESCRIPTION = "description";
  static final String PARAM_PATTERN = "projectKeyPattern";

  static final String PARAM_QUALIFIER = "qualifier";

  private static final String PERMISSION_PARAM_DESCRIPTION = format("Permission" +
    "<ul>" +
    "<li>Possible values for global permissions: %s</li>" +
    "<li>Possible values for project permissions %s</li>" +
    "</ul>",
    GlobalPermissions.ALL_ON_ONE_LINE,
    ProjectPermissions.ALL_ON_ONE_LINE);
  private static final String PROJECT_PERMISSION_PARAM_DESCRIPTION = format("Permission" +
    "<ul>" +
    "<li>Possible values for project permissions %s</li>" +
    "</ul>",
    ProjectPermissions.ALL_ON_ONE_LINE);

  private WsPermissionParameters() {
    // static methods only
  }

  static void createPermissionParameter(NewAction action) {
    action.createParam(PARAM_PERMISSION)
      .setDescription(PERMISSION_PARAM_DESCRIPTION)
      .setRequired(true);
  }

  static void createProjectPermissionParameter(NewAction action) {
    action.createParam(PARAM_PERMISSION)
      .setDescription(PROJECT_PERMISSION_PARAM_DESCRIPTION)
      .setRequired(true);
  }

  static void createGroupNameParameter(NewAction action) {
    action.createParam(PARAM_GROUP_NAME)
      .setDescription("Group name or 'anyone' (case insensitive)")
      .setExampleValue("sonar-administrators");
  }

  static void createGroupIdParameter(NewAction action) {
    action.createParam(PARAM_GROUP_ID)
      .setDescription("Group id")
      .setExampleValue("42");
  }

  static void createProjectParameter(NewAction action) {
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
      .setExampleValue("org.apache.hbas:hbase");
  }

  static void createUserLoginParameter(NewAction action) {
    action.createParam(PARAM_USER_LOGIN)
      .setRequired(true)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }

  static void createTemplateParameters(NewAction action) {
    createTemplateIdParameter(action);
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

  static void createTemplateProjectKeyPatternParameter(NewAction action) {
    action.createParam(PARAM_PATTERN)
      .setDescription("Project key pattern. Must be a valid Java regular expression")
      .setExampleValue(".*\\.finance\\..*");
  }

  static void createTemplateDescriptionParameter(NewAction action) {
    action.createParam(PARAM_DESCRIPTION)
      .setDescription("Description")
      .setExampleValue("Permissions for all projects related to the financial service");
  }

  static void createIdParameter(NewAction action) {
    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("Id")
      .setExampleValue("af8cb8cc-1e78-4c4e-8c00-ee8e814009a5");
  }
}
