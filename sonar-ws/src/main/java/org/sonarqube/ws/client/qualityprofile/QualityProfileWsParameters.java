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
package org.sonarqube.ws.client.qualityprofile;

public class QualityProfileWsParameters {

  public static final String CONTROLLER_QUALITY_PROFILES = "api/qualityprofiles";

  public interface RestoreActionParameters {

    String PARAM_BACKUP = "backup";
  }
  public static final String ACTION_ACTIVATE_RULE = "activate_rule";
  public static final String ACTION_ACTIVATE_RULES = "activate_rules";
  public static final String ACTION_ADD_PROJECT = "add_project";
  public static final String ACTION_ADD_GROUP = "add_group";
  public static final String ACTION_ADD_USER = "add_user";
  public static final String ACTION_CHANGE_PARENT = "change_parent";
  public static final String ACTION_COPY = "copy";
  public static final String ACTION_CREATE = "create";
  public static final String ACTION_DEACTIVATE_RULE = "deactivate_rule";
  public static final String ACTION_DEACTIVATE_RULES = "deactivate_rules";
  public static final String ACTION_DELETE = "delete";
  public static final String ACTION_REMOVE_PROJECT = "remove_project";
  public static final String ACTION_REMOVE_GROUP = "remove_group";
  public static final String ACTION_REMOVE_USER = "remove_user";
  public static final String ACTION_RESTORE = "restore";
  public static final String ACTION_SEARCH = "search";
  public static final String ACTION_SEARCH_USERS = "search_users";
  public static final String ACTION_SEARCH_GROUPS = "search_groups";
  public static final String ACTION_SHOW = "show";
  public static final String ACTION_SET_DEFAULT = "set_default";

  public static final String PARAM_COMPARE_TO_SONAR_WAY = "compareToSonarWay";
  public static final String PARAM_DEFAULTS = "defaults";
  public static final String PARAM_FROM_KEY = "fromKey";
  public static final String PARAM_GROUP = "group";
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_LANGUAGE = "language";
  public static final String PARAM_LOGIN = "login";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_PARAMS = "params";
  public static final String PARAM_PARENT_KEY = "parentKey";
  public static final String PARAM_PARENT_QUALITY_PROFILE = "parentQualityProfile";
  public static final String PARAM_KEY = "key";
  public static final String PARAM_QUALITY_PROFILE = "qualityProfile";
  public static final String PARAM_PROJECT = "project";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_PROJECT_UUID = "projectUuid";
  public static final String PARAM_QUERY = "q";
  public static final String PARAM_RESET = "reset";
  public static final String PARAM_RULE = "rule";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_SINCE = "since";
  public static final String PARAM_TARGET_KEY = "targetKey";
  public static final String PARAM_TARGET_SEVERITY = "targetSeverity";
  public static final String PARAM_TO = "to";
  public static final String PARAM_TO_NAME = "toName";

  private QualityProfileWsParameters() {
    // Only static stuff
  }

}
