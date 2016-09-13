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
package org.sonarqube.ws.client.qualityprofile;

public class QualityProfileWsParameters {

  public static final String CONTROLLER_QUALITY_PROFILES = "api/qualityprofiles";

  public static final String ACTION_SEARCH = "search";
  public static final String ACTION_ADD_PROJECT = "add_project";
  public static final String ACTION_REMOVE_PROJECT = "remove_project";
  public static final String ACTION_CREATE = "create";

  public static final String PARAM_DEFAULTS = "defaults";
  public static final String PARAM_LANGUAGE = "language";
  public static final String PARAM_PROFILE_NAME = "profileName";
  public static final String PARAM_PROFILE_KEY = "profileKey";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_PROJECT_UUID = "projectUuid";

  private QualityProfileWsParameters() {
    // Only static stuff
  }

}
