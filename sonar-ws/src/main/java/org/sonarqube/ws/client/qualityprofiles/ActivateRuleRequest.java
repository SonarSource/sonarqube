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
package org.sonarqube.ws.client.qualityprofiles;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualityprofiles/activate_rule">Further information about this action online (including a response example)</a>
 * @since 4.4
 */
@Generated("sonar-ws-generator")
public class ActivateRuleRequest {

  private String key;
  private List<String> params;
  private String reset;
  private String rule;
  private String severity;

  /**
   * This is a mandatory parameter.
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public ActivateRuleRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Example value: "params=key1=v1;key2=v2"
   */
  public ActivateRuleRequest setParams(List<String> params) {
    this.params = params;
    return this;
  }

  public List<String> getParams() {
    return params;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public ActivateRuleRequest setReset(String reset) {
    this.reset = reset;
    return this;
  }

  public String getReset() {
    return reset;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "squid:AvoidCycles"
   */
  public ActivateRuleRequest setRule(String rule) {
    this.rule = rule;
    return this;
  }

  public String getRule() {
    return rule;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"INFO"</li>
   *   <li>"MINOR"</li>
   *   <li>"MAJOR"</li>
   *   <li>"CRITICAL"</li>
   *   <li>"BLOCKER"</li>
   * </ul>
   */
  public ActivateRuleRequest setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String getSeverity() {
    return severity;
  }
}
