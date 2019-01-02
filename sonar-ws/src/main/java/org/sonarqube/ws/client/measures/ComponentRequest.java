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
package org.sonarqube.ws.client.measures;

import java.util.List;
import javax.annotation.Generated;

/**
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
  private List<String> metricKeys;
  private String pullRequest;

  /**
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
   * This is a mandatory parameter.
   * Example value: "ncloc,complexity,violations"
   */
  public ComponentRequest setMetricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  /**
   * This is part of the internal API.
   * Example value: "5461"
   */
  public ComponentRequest setPullRequest(String pullRequest) {
    this.pullRequest = pullRequest;
    return this;
  }

  public String getPullRequest() {
    return pullRequest;
  }
}
