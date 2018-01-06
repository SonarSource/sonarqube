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
package org.sonar.server.ce.ws;

public class CeWsParameters {

  public static final String ACTION_WORKER_COUNT = "worker_count";

  public static final String PARAM_COMPONENT_ID = "componentId";
  public static final String DEPRECATED_PARAM_COMPONENT_KEY = "componentKey";
  public static final String PARAM_COMPONENT = "component";
  public static final String PARAM_COMPONENT_QUERY = "componentQuery";
  public static final String PARAM_TYPE = "type";
  public static final String PARAM_STATUS = "status";
  public static final String PARAM_ONLY_CURRENTS = "onlyCurrents";
  public static final String PARAM_MIN_SUBMITTED_AT = "minSubmittedAt";
  public static final String PARAM_MAX_EXECUTED_AT = "maxExecutedAt";

  private CeWsParameters() {
    // prevent instantiation
  }
}
