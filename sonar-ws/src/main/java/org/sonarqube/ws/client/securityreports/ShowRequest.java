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
package org.sonarqube.ws.client.securityreports;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/security_reports/show">Further information about this action online (including a response example)</a>
 * @since 7.3
 */
@Generated("sonar-ws-generator")
public class ShowRequest {

  private String branch;
  private String includeDistribution;
  private String project;
  private String standard;

  /**
   * Example value: "branch-2.0"
   */
  public ShowRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
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
  public ShowRequest setIncludeDistribution(String includeDistribution) {
    this.includeDistribution = includeDistribution;
    return this;
  }

  public String getIncludeDistribution() {
    return includeDistribution;
  }

  /**
   * This is a mandatory parameter.
   */
  public ShowRequest setProject(String project) {
    this.project = project;
    return this;
  }

  public String getProject() {
    return project;
  }

  /**
   * This is a mandatory parameter.
   * Possible values:
   * <ul>
   *   <li>"owaspTop10"</li>
   *   <li>"sansTop25"</li>
   * </ul>
   */
  public ShowRequest setStandard(String standard) {
    this.standard = standard;
    return this;
  }

  public String getStandard() {
    return standard;
  }
}
