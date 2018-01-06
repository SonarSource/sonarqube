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
package org.sonarqube.ws.client.user;

public class UsersWsParameters {

  public static final String CONTROLLER_USERS = "api/users";

  public static final String ACTION_SEARCH = "search";
  public static final String ACTION_CREATE = "create";
  public static final String ACTION_DEACTIVATE = "deactivate";
  public static final String ACTION_UPDATE = "update";
  public static final String ACTION_GROUPS = "groups";
  public static final String ACTION_SKIP_ONBOARDING_TUTORIAL = "skip_onboarding_tutorial";
  public static final String ACTION_CURRENT = "current";

  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_LOGIN = "login";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_EMAIL = "email";
  public static final String PARAM_SCM_ACCOUNTS = "scmAccounts";
  public static final String PARAM_SCM_ACCOUNTS_DEPRECATED = "scm_accounts";
  public static final String PARAM_SCM_ACCOUNT = "scmAccount";
  public static final String PARAM_LOCAL = "local";
  public static final String PARAM_SELECTED = "selected";

  private UsersWsParameters() {
    // Only static stuff
  }

}
