/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.List;
import javax.annotation.Generated;

/**
 * Add a new condition to a quality gate.<br>Requires the 'Administer Quality Gates' permission.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/create_condition">Further information about this action online (including a response example)</a>
 * @since 4.3
 */
@Generated("sonar-ws-generator")
public class CreateConditionRequest {

  private String error;
  private String gateId;
  private String metric;
  private String op;
  private String period;
  private String warning;

  /**
   * Condition error threshold
   *
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
   * ID of the quality gate
   *
   * This is a mandatory parameter.
   * Example value: "1"
   */
  public CreateConditionRequest setGateId(String gateId) {
    this.gateId = gateId;
    return this;
  }

  public String getGateId() {
    return gateId;
  }

  /**
   * Condition metric
   *
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
   * Condition operator:<br/><ul><li>EQ = equals</li><li>NE = is not</li><li>LT = is lower than</li><li>GT = is greater than</li></ui>
   *
   * Example value: "EQ"
   * Possible values:
   * <ul>
   *   <li>"LT"</li>
   *   <li>"GT"</li>
   *   <li>"EQ"</li>
   *   <li>"NE"</li>
   * </ul>
   */
  public CreateConditionRequest setOp(String op) {
    this.op = op;
    return this;
  }

  public String getOp() {
    return op;
  }

  /**
   * Condition period. If not set, the absolute value is considered.
   *
   * Possible values:
   * <ul>
   *   <li>"1"</li>
   * </ul>
   */
  public CreateConditionRequest setPeriod(String period) {
    this.period = period;
    return this;
  }

  public String getPeriod() {
    return period;
  }

  /**
   * Condition warning threshold
   *
   * Example value: "5"
   */
  public CreateConditionRequest setWarning(String warning) {
    this.warning = warning;
    return this;
  }

  public String getWarning() {
    return warning;
  }
}
