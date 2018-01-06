/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/project_status">Further information about this action online (including a response example)</a>
 * @since 5.3
 */
@Generated("sonar-ws-generator")
public class ProjectStatusRequest {

  private String analysisId;
  private String projectId;
  private String projectKey;

  /**
   * Example value: "AU-TpxcA-iU5OvuD2FL1"
   */
  public ProjectStatusRequest setAnalysisId(String analysisId) {
    this.analysisId = analysisId;
    return this;
  }

  public String getAnalysisId() {
    return analysisId;
  }

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public ProjectStatusRequest setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getProjectId() {
    return projectId;
  }

  /**
   * Example value: "my_project"
   */
  public ProjectStatusRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }
}
