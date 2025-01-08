/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.client.projects;

import jakarta.annotation.Generated;
import javax.annotation.Nullable;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/export_findings">Further information about this action online (including a response example)</a>
 * @since 9.1
 */
@Generated("sonar-ws-generator")
public class ExportFindingsRequest {

  private String projectKey;
  private String branchKey;

  public ExportFindingsRequest(String projectKey, @Nullable String branchKey) {
    this.projectKey = projectKey;
    this.branchKey = branchKey;
  }

  /**
   * Example value: "42"
   */
  public ExportFindingsRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Example value: "sonar"
   */
  public ExportFindingsRequest setBranchKey(String branchKey) {
    this.branchKey = branchKey;
    return this;
  }

  public String getBranchKey() {
    return branchKey;
  }

}
