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
package org.sonar.server.qualitygate.ws;

public class QualityGatesWsParameters {

  public static final String CONTROLLER_QUALITY_GATES = "api/qualitygates";

  public static final String ACTION_PROJECT_STATUS = "project_status";
  public static final String ACTION_GET_BY_PROJECT = "get_by_project";
  public static final String ACTION_SELECT = "select";
  public static final String ACTION_CREATE = "create";
  public static final String ACTION_CREATE_CONDITION = "create_condition";
  public static final String ACTION_UPDATE_CONDITION = "update_condition";

  static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_ANALYSIS_ID = "analysisId";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_PAGE_SIZE = "pageSize";
  public static final String PARAM_PAGE = "page";
  public static final String PARAM_QUERY = "query";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_ERROR = "error";
  public static final String PARAM_WARNING = "warning";
  public static final String PARAM_PERIOD = "period";
  public static final String PARAM_OPERATOR = "op";
  public static final String PARAM_METRIC = "metric";
  public static final String PARAM_GATE_ID = "gateId";
  public static final String PARAM_ID = "id";

  private QualityGatesWsParameters() {
    // prevent instantiation
  }

}
