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
package org.sonarqube.ws.client.governancereports;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/governance_reports/update_frequency">Further information about this action online (including a response example)</a>
 * @since 1.0
 */
@Generated("sonar-ws-generator")
public class UpdateFrequencyRequest {

  private String componentId;
  private String componentKey;
  private String frequency;

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public UpdateFrequencyRequest setComponentId(String componentId) {
    this.componentId = componentId;
    return this;
  }

  public String getComponentId() {
    return componentId;
  }

  /**
   * Example value: "my_project"
   */
  public UpdateFrequencyRequest setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public String getComponentKey() {
    return componentKey;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"daily"</li>
   *   <li>"weekly"</li>
   *   <li>"monthly"</li>
   * </ul>
   */
  public UpdateFrequencyRequest setFrequency(String frequency) {
    this.frequency = frequency;
    return this;
  }

  public String getFrequency() {
    return frequency;
  }
}
