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
package org.sonarqube.ws.client.custommeasures;

import java.util.List;
import javax.annotation.Generated;

/**
 * Create a custom measure.<br /> The project id or the project key must be provided (only project and module custom measures can be created). The metric id or the metric key must be provided.<br/>Requires 'Administer System' permission or 'Administer' permission on the project.
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/custom_measures/create">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class CreateRequest {

  private String description;
  private String metricId;
  private String metricKey;
  private String projectId;
  private String projectKey;
  private String value;

  /**
   * Description
   *
   * Example value: "Team size growing."
   */
  public CreateRequest setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Metric id
   *
   * Example value: "16"
   */
  public CreateRequest setMetricId(String metricId) {
    this.metricId = metricId;
    return this;
  }

  public String getMetricId() {
    return metricId;
  }

  /**
   * Metric key
   *
   * Example value: "ncloc"
   */
  public CreateRequest setMetricKey(String metricKey) {
    this.metricKey = metricKey;
    return this;
  }

  public String getMetricKey() {
    return metricKey;
  }

  /**
   * Project id
   *
   * Example value: "ce4c03d6-430f-40a9-b777-ad877c00aa4d"
   */
  public CreateRequest setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getProjectId() {
    return projectId;
  }

  /**
   * Project key
   *
   * Example value: "my_project"
   */
  public CreateRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Measure value. Value type depends on metric type:<ul><li>INT - type: integer</li><li>FLOAT - type: double</li><li>PERCENT - type: double</li><li>BOOL - the possible values are true or false</li><li>STRING - type: string</li><li>MILLISEC - type: integer</li><li>DATA - type: string</li><li>LEVEL - the possible values are OK, WARN, ERROR</li><li>DISTRIB - type: string</li><li>RATING - type: double</li><li>WORK_DUR - long representing the number of minutes</li></ul>
   *
   * This is a mandatory parameter.
   * Example value: "47"
   */
  public CreateRequest setValue(String value) {
    this.value = value;
    return this;
  }

  public String getValue() {
    return value;
  }
}
