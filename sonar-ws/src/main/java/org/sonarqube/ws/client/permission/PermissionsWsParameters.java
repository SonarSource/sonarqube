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
package org.sonarqube.ws.client.permission;

public class PermissionsWsParameters {
  public static final String CONTROLLER = "api/permissions";

  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_GROUP_NAME = "groupName";
  public static final String PARAM_GROUP_ID = "groupId";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_USER_LOGIN = "login";
  public static final String PARAM_TEMPLATE_ID = "templateId";
  public static final String PARAM_TEMPLATE_NAME = "templateName";
  public static final String PARAM_ID = "id";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_DESCRIPTION = "description";
  public static final String PARAM_PROJECT_KEY_PATTERN = "projectKeyPattern";
  public static final String PARAM_QUALIFIER = "qualifier";

  private PermissionsWsParameters() {
    // static utils only
  }
}
