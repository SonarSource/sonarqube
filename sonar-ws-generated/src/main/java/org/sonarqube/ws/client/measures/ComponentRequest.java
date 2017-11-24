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
package org.sonarqube.ws.client.measures;

import java.util.List;
import javax.annotation.Generated;

/**
 * Return component with specified measures. The componentId or the component parameter must be provided.<br>Requires the following permission: 'Browse' on the project of specified component.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/measures/component">Further information about this action online (including a response example)</a>
 * @since 5.4
 */
@Generated("sonar-ws-generator")
public class ComponentRequest {

  private List<String> additionalFields;
  private String branch;
  private String component;
  private String componentId;
  private String developerId;
  private String developerKey;
  private String metricKeys;

  /**
   * Comma-separated list of additional fields that can be returned in the response.
   *
   * Example value: "periods,metrics"
   * Possible values:
   * <ul>
   *   <li>"metrics"</li>
   *   <li>"periods"</li>
   * </ul>
   */
  public ComponentRequest setAdditionalFields(List<String> additionalFields) {
    this.additionalFields = additionalFields;
    return this;
  }

  public List<String> getAdditionalFields() {
    return additionalFields;
  }

  /**
   * Branch key
   *
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public ComponentRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Component key
   *
   * Example value: "my_project"
   */
  public ComponentRequest setComponent(String component) {
    this.component = component;
    return this;
  }

  public String getComponent() {
    return component;
  }

  /**
   * Component id
   *
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   * @deprecated since 6.6
   */
  @Deprecated
  public ComponentRequest setComponentId(String componentId) {
    this.componentId = componentId;
    return this;
  }

  public String getComponentId() {
    return componentId;
  }

  /**
   * Deprecated parameter, used previously with the Developer Cockpit plugin. No measures are returned if parameter is set.
   *
   * @deprecated since 6.4
   */
  @Deprecated
  public ComponentRequest setDeveloperId(String developerId) {
    this.developerId = developerId;
    return this;
  }

  public String getDeveloperId() {
    return developerId;
  }

  /**
   * Deprecated parameter, used previously with the Developer Cockpit plugin. No measures are returned if parameter is set.
   *
   * @deprecated since 6.4
   */
  @Deprecated
  public ComponentRequest setDeveloperKey(String developerKey) {
    this.developerKey = developerKey;
    return this;
  }

  public String getDeveloperKey() {
    return developerKey;
  }

  /**
   * Metric keys
   *
   * This is a mandatory parameter.
   * Example value: "ncloc,complexity,violations"
   */
  public ComponentRequest setMetricKeys(String metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  public String getMetricKeys() {
    return metricKeys;
  }
}
