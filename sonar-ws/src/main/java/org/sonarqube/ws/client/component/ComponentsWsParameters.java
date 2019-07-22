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
package org.sonarqube.ws.client.component;

public class ComponentsWsParameters {

  public static final String CONTROLLER_COMPONENTS = "api/components";

  // actions
  public static final String ACTION_SEARCH = "search";
  public static final String ACTION_TREE = "tree";
  public static final String ACTION_SHOW = "show";
  public static final String ACTION_SEARCH_PROJECTS = "search_projects";
  public static final String ACTION_SUGGESTIONS = "suggestions";

  // parameters
  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_QUALIFIERS = "qualifiers";
  public static final String PARAM_LANGUAGE = "language";
  public static final String PARAM_STRATEGY = "strategy";
  public static final String PARAM_FILTER = "filter";
  public static final String PARAM_COMPONENT = "component";
  public static final String PARAM_BRANCH = "branch";
  public static final String PARAM_PULL_REQUEST = "pullRequest";

  private ComponentsWsParameters() {
    // static utility class
  }
}
