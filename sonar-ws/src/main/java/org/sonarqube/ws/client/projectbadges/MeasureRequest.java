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
package org.sonarqube.ws.client.projectbadges;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/project_badges/measure">Further information about this action online (including a response example)</a>
 * @since 7.1
 */
@Generated("sonar-ws-generator")
public class MeasureRequest {

  private String branch;
  private String metric;
  private String project;

  /**
   * Example value: "feature/my_branch"
   */
  public MeasureRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * This is a mandatory parameter.
   * Possible values:
   * <ul>
   *   <li>"bugs"</li>
   *   <li>"code_smells"</li>
   *   <li>"coverage"</li>
   *   <li>"duplicated_lines_density"</li>
   *   <li>"ncloc"</li>
   *   <li>"sqale_rating"</li>
   *   <li>"alert_status"</li>
   *   <li>"reliability_rating"</li>
   *   <li>"security_rating"</li>
   *   <li>"sqale_index"</li>
   *   <li>"vulnerabilities"</li>
   * </ul>
   */
  public MeasureRequest setMetric(String metric) {
    this.metric = metric;
    return this;
  }

  public String getMetric() {
    return metric;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public MeasureRequest setProject(String project) {
    this.project = project;
    return this;
  }

  public String getProject() {
    return project;
  }
}
