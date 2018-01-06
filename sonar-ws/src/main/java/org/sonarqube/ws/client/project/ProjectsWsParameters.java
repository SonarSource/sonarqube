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
package org.sonarqube.ws.client.project;

public class ProjectsWsParameters {

  public static final int MAX_PAGE_SIZE = 500;

  public static final String CONTROLLER = "api/projects";

  public static final String ACTION_CREATE = "create";
  public static final String ACTION_INDEX = "index";
  public static final String ACTION_SEARCH = "search";
  public static final String ACTION_UPDATE_KEY = "update_key";
  public static final String ACTION_BULK_UPDATE_KEY = "bulk_update_key";
  public static final String ACTION_UPDATE_VISIBILITY = "update_visibility";

  public static final String PARAM_PROJECT = "project";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_BRANCH = "branch";
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_QUALIFIERS = "qualifiers";
  public static final String PARAM_FROM = "from";
  public static final String PARAM_TO = "to";
  public static final String PARAM_DRY_RUN = "dryRun";
  public static final String PARAM_VISIBILITY = "visibility";
  public static final String PARAM_ANALYZED_BEFORE = "analyzedBefore";
  public static final String PARAM_ON_PROVISIONED_ONLY = "onProvisionedOnly";
  public static final String PARAM_PROJECT_IDS = "projectIds";
  public static final String PARAM_PROJECTS = "projects";

  public static final String FILTER_LANGUAGES = "languages";
  public static final String FILTER_TAGS = "tags";

  private ProjectsWsParameters() {
    // static utils only
  }
}
