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
package org.sonarqube.ws.client.ce;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/ce/submit">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class SubmitRequest {

  private String characteristic;
  private String organization;
  private String projectBranch;
  private String projectKey;
  private String projectName;
  private String report;

  /**
   * Example value: "branchType=long"
   */
  public SubmitRequest setCharacteristic(String characteristic) {
    this.characteristic = characteristic;
    return this;
  }

  public String getCharacteristic() {
    return characteristic;
  }

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public SubmitRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "branch-1.x"
   */
  public SubmitRequest setProjectBranch(String projectBranch) {
    this.projectBranch = projectBranch;
    return this;
  }

  public String getProjectBranch() {
    return projectBranch;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "my_project"
   */
  public SubmitRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Example value: "My Project"
   */
  public SubmitRequest setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  /**
   * This is a mandatory parameter.
   */
  public SubmitRequest setReport(String report) {
    this.report = report;
    return this;
  }

  public String getReport() {
    return report;
  }
}
