/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.ws.client.qualitygates;

import jakarta.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/create_condition">Further information about this action online (including a response example)</a>
 * @since 4.3
 */
@Generated("sonar-ws-generator")
public class CreateConditionRequest {

  private String error;
  private String gateName;
  private String metric;
  private String op;

  /**
   * Example value: "10"
   */
  public CreateConditionRequest setError(String error) {
    this.error = error;
    return this;
  }

  public String getError() {
    return error;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "1"
   */
  public CreateConditionRequest setGateName(String gateName) {
    this.gateName = gateName;
    return this;
  }

  public String getGateName() {
    return gateName;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "blocker_violations"
   */
  public CreateConditionRequest setMetric(String metric) {
    this.metric = metric;
    return this;
  }

  public String getMetric() {
    return metric;
  }

  /**
   * Example value: "LT"
   * Possible values:
   * <ul>
   *   <li>"LT"</li>
   *   <li>"GT"</li>
   * </ul>
   */
  public CreateConditionRequest setOp(String op) {
    this.op = op;
    return this;
  }

  public String getOp() {
    return op;
  }
}
